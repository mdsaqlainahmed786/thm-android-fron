package com.thehotelmedia.android.fragments.userTypes.individual.bottomNavigation

import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.graphics.Rect
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
    private var activePosition = RecyclerView.NO_POSITION // No active position initially
    private var currentLat = DEFAULT_LAT
    private var currentLng = DEFAULT_LNG
    private val batchSize = 30
    private val handler = Handler(Looper.getMainLooper())
    private var isScrolling = false
     private var isManualRefresh = false
     private var loadStateListenerAdded = false
    private var lastScrollY = 0
    private var isScrollingDown = true // Default to true for initial load
    // Removed debounceRunnable - we now use continuous scroll detection via viewTreeObserver
    // which is more reliable for video auto-play


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
            // Explicit user-initiated refresh: always go to top of the feed
            isManualRefresh = true
            binding.postRecyclerView.scrollToPosition(0)
//            getStoryData()
//            refreshHandler.removeCallbacksAndMessages(null)
            getFeedData("refresh")
        }

        // Let SwipeRefreshLayout decide when pull-to-refresh should be possible based on whether
        // the RecyclerView can scroll up. This is more reliable than manually toggling
        // swipeRefreshLayout.isEnabled using fragile top/offset checks.
        binding.swipeRefreshLayout.setOnChildScrollUpCallback { _, _ ->
            binding.postRecyclerView.canScrollVertically(-1)
        }

//        binding.switchThemeButton.setOnClickListener {
//            ThemeHelper.toggleTheme(requireActivity())
//            requireActivity().recreate() // Restart activity to apply theme change
//        }

        // Fragment Result Listener to receive data
        parentFragmentManager.setFragmentResultListener("home_key", this) { _, bundle ->
            val refresh = bundle.getString("refresh")
            binding.postRecyclerView.scrollToPosition(0)
        }

        // Fragment Result Listener to scroll to a specific post
        parentFragmentManager.setFragmentResultListener("scroll_to_post", this) { _, bundle ->
            val postId = bundle.getString("SCROLL_TO_POST_ID")
            if (!postId.isNullOrBlank()) {
                scrollToPost(postId)
            }

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


        // NOTE: We intentionally do NOT toggle swipeRefreshLayout.isEnabled based on scroll position.
        // Doing so can accidentally disable pull-to-refresh if the top item isn't aligned to top==0
        // (e.g., due to padding, decorations, or header layout changes). The child-scroll callback
        // above handles this reliably.

        individualViewModal.toast.observe(viewLifecycleOwner){
            CustomSnackBar.showSnackBar(binding.root,it)
        }
        individualViewModal.reportToast.observe(viewLifecycleOwner){
//            CustomSnackBar.showSnackBar(binding.root,it)
            Toast.makeText(activity,it, Toast.LENGTH_SHORT).show()
        }
        
        // Observe story publish result and refresh stories list
        individualViewModal.publishStoryResult.observe(viewLifecycleOwner) { result ->
            if (result?.status == true) {
                // Story published successfully, refresh the stories list
                feedAdapter.refreshStories()
            }
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
        // Stop inline video playback when user navigates away from this screen
        VideoPlayerManager.pausePlayer()
        super.onPause()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        // Fully release the player so audio cannot continue in background
        VideoPlayerManager.releasePlayer()
    }
    
    override fun onResume() {
        super.onResume()
        // Refresh the feed adapter when returning from other activities (like ViewStoriesActivity)
        // This ensures the story rings update immediately after viewing stories
        if (binding.postRecyclerView.adapter != null && !isScrolling) {
            // Only refresh if not currently scrolling to avoid scroll position jumps
            binding.postRecyclerView.post {
                // Check if still not scrolling before refreshing
                if (!isScrolling) {
                    val headerViewHolder = binding.postRecyclerView.findViewHolderForAdapterPosition(0)
                    if (headerViewHolder is FeedAdapter.HeaderViewHolder) {
                        // Use the new refresh method that doesn't re-submit data
                        headerViewHolder.refreshStoryRings()
                    }
                    // Removed notifyItemChanged(0) to prevent scroll jumps
                }
            }
        }
    }

    private fun getFeedData(refresh: String) {

        if (binding.postRecyclerView.adapter == null) {
            binding.postRecyclerView.adapter = feedAdapter.withLoadStateFooter(
                footer = LoaderAdapter { feedAdapter.retry() }
            )
            binding.postRecyclerView.isNestedScrollingEnabled = false
            binding.postRecyclerView.setItemViewCacheSize(10) // Increased cache size for smoother scrolling
            binding.postRecyclerView.setHasFixedSize(false) // Allow RecyclerView to optimize layout
            binding.postRecyclerView.itemAnimator = null

            // (Child scroll callback is configured in initUI for reliability across all states.)
            
            // Continuous scroll detection for video auto-play (similar to ProfilePostsFragment)
            // This fires whenever the RecyclerView scrolls, allowing us to track which post
            // is most visible and should have its video auto-playing.
            binding.postRecyclerView.viewTreeObserver.addOnScrollChangedListener {
                val recyclerView = binding.postRecyclerView
                val candidatePosition = findMostVisibleItemPosition(recyclerView)
                if (candidatePosition != RecyclerView.NO_POSITION && candidatePosition != activePosition) {
                    recyclerView.post {
                        if (candidatePosition != activePosition) {
                            updateActivePosition(candidatePosition)
                        }
                    }
                }
            }
            
            // Keep scroll listener only for scroll direction tracking and state
            binding.postRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    isScrolling = true
                    
                    // Track scroll direction
                    val currentScrollY = recyclerView.computeVerticalScrollOffset()
                    isScrollingDown = currentScrollY > lastScrollY
                    lastScrollY = currentScrollY
                }
                
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)
                    when (newState) {
                        RecyclerView.SCROLL_STATE_IDLE -> {
                            isScrolling = false
                        }
                        RecyclerView.SCROLL_STATE_DRAGGING, RecyclerView.SCROLL_STATE_SETTLING -> {
                            isScrolling = true
                        }
                    }
                }
            })

            // Initial Observation - Only observe ONCE
            individualViewModal.getFeeds(currentLat,currentLng).observe(viewLifecycleOwner) { data ->
                lifecycleScope.launch {
                    ensureLoadStateListener()
                    feedAdapter.submitData(data)
                    // After initial data load, ensure the first visible post (after header)
                    // is marked active so its video can auto‑play.
                    binding.postRecyclerView.post {
                        val initialPosition = findMostVisibleItemPosition(binding.postRecyclerView)
                        if (initialPosition != RecyclerView.NO_POSITION) {
                            updateActivePosition(initialPosition)
                        }
                    }
                }
            }
        } else {
             // For subsequent refreshes, just refresh the adapter
            feedAdapter.refresh()
            feedAdapter.refreshStories()
        }
    }

    /**
     * Returns the adapter position of the item that is mostly visible on screen.
     * Skips position 0 (header) and finds the most visible post item.
     */
    private fun findMostVisibleItemPosition(recyclerView: RecyclerView): Int {
        val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return RecyclerView.NO_POSITION

        val firstVisible = layoutManager.findFirstVisibleItemPosition()
        val lastVisible = layoutManager.findLastVisibleItemPosition()
        if (firstVisible == RecyclerView.NO_POSITION || lastVisible == RecyclerView.NO_POSITION) {
            return RecyclerView.NO_POSITION
        }

        // Item becomes "active" when ~60% of its height is visible on screen
        val visibilityThreshold = 0.6f
        var bestPosition = RecyclerView.NO_POSITION
        var maxVisibleRatio = 0f

        val parentGlobalRect = Rect()
        recyclerView.getGlobalVisibleRect(parentGlobalRect)

        val childGlobalRect = Rect()

        for (position in firstVisible..lastVisible) {
            // Skip header (position 0)
            if (position == 0) continue
            
            val child = layoutManager.findViewByPosition(position) ?: continue
            if (child.height <= 0) continue

            val hasVisibleRect = child.getGlobalVisibleRect(childGlobalRect)
            if (!hasVisibleRect) continue

            val visibleRect = Rect(childGlobalRect)
            val intersected = visibleRect.intersect(parentGlobalRect)
            if (!intersected) continue

            val visibleHeight = visibleRect.height().coerceAtLeast(0)
            if (visibleHeight <= 0) continue

            val ratio = visibleHeight.toFloat() / child.height.toFloat()
            if (ratio > maxVisibleRatio) {
                maxVisibleRatio = ratio
                bestPosition = position
            }
        }

        return if (maxVisibleRatio >= visibilityThreshold) bestPosition else RecyclerView.NO_POSITION
    }

    private fun updateActivePosition(newPosition: Int) {
        if (newPosition == activePosition) return
        if (newPosition < 0 || newPosition >= feedAdapter.itemCount) return

        activePosition = newPosition
        // Delegate item update logic to the adapter so it can decide which post
        // should have its media (video/image) in the active state.
        // Pass scroll direction so adapter can control buffering indicator
        feedAdapter.setActivePosition(newPosition, isScrollingDown)
    }

    private fun ensureLoadStateListener() {
        if (loadStateListenerAdded) return
        loadStateListenerAdded = true

        feedAdapter.addLoadStateListener { loadState ->
            val refreshState = loadState.refresh
            val appendState = loadState.append
            val isLoading = refreshState is LoadState.Loading
            val isEmpty = refreshState is LoadState.NotLoading && feedAdapter.itemCount == 0

            if (isLoading) {
//                // Show progress bar only for non-refresh loads, if needed
//                if (!binding.swipeRefreshLayout.isRefreshing) {
//                    progressBar.show()
//                }
                
                // Add a failsafe to stop refreshing after 10 seconds if it gets stuck
                if (binding.swipeRefreshLayout.isRefreshing) {
                    Handler(Looper.getMainLooper()).postDelayed({
                        if (binding.swipeRefreshLayout.isRefreshing) {
                            binding.swipeRefreshLayout.isRefreshing = false
                            progressBar.hide()
                        }
                    }, 10000) // 10 seconds timeout
                }
            } else {
                // Hide progress bar and refresh indicator when loading completes
                Handler(Looper.getMainLooper()).post {
                    progressBar.hide()
                    binding.swipeRefreshLayout.isRefreshing = false

                    // Handle errors
                    if (refreshState is LoadState.Error) {
                        val errorState = refreshState
                        Toast.makeText(requireContext(), "Error: ${errorState.error.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
                    // Handle "load more" (append) errors so users don't see a silent spinner.
                    if (appendState is LoadState.Error) {
                        Toast.makeText(
                            requireContext(),
                            "Error loading more: ${appendState.error.localizedMessage}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    // After a manual refresh (user pulled to refresh), keep list at the very top.
                    // Guard with isManualRefresh so normal paging loads while scrolling don't jump to top.
                    if (isManualRefresh) {
                        isManualRefresh = false
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

    private fun scrollToPost(postId: String) {
        // Wait for adapter to load items, then scroll to the post
        viewLifecycleOwner.lifecycleScope.launch {
            var attempts = 0
            while (attempts < 50) { // Max 5 seconds
                val itemCount = feedAdapter.itemCount
                if (itemCount > 1) { // More than just header
                    val position = feedAdapter.findPostPosition(postId)
                    if (position >= 0) {
                        binding.postRecyclerView.post {
                            binding.postRecyclerView.scrollToPosition(position)
                            // Update active position after scrolling
                            binding.postRecyclerView.postDelayed({
                                val layoutManager = binding.postRecyclerView.layoutManager as? LinearLayoutManager
                                val firstVisible = layoutManager?.findFirstCompletelyVisibleItemPosition() ?: -1
                                if (firstVisible >= 0) {
                                    updateActivePosition(firstVisible)
                                }
                            }, 300)
                        }
                        return@launch
                    }
                }
                kotlinx.coroutines.delay(100)
                attempts++
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
        val SWIPE_THRESHOLD = 150f // Increased threshold - must swipe at least 150px
        val DETECTION_THRESHOLD = 60f // Increased detection threshold - must move at least 60px horizontally
        val MIN_HORIZONTAL_RATIO = 2.5f // Horizontal must be at least 2.5x the vertical movement
        val MAX_VERTICAL_FOR_HORIZONTAL = 40f // If vertical movement exceeds this, don't consider it horizontal
        
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
                    
                    // Only detect horizontal swipe if:
                    // 1. Horizontal movement is significant (>= DETECTION_THRESHOLD)
                    // 2. Horizontal is at least MIN_HORIZONTAL_RATIO times the vertical movement
                    // 3. Vertical movement hasn't exceeded MAX_VERTICAL_FOR_HORIZONTAL (to avoid false positives during scrolling)
                    if (!isHorizontalSwipe && 
                        absDeltaX > DETECTION_THRESHOLD && 
                        absDeltaX > absDeltaY * MIN_HORIZONTAL_RATIO &&
                        absDeltaY < MAX_VERTICAL_FOR_HORIZONTAL) {
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
                        
                        // Check if swipe distance meets threshold (must be a complete swipe)
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
