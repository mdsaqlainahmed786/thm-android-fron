package com.thehotelmedia.android.activity.userTypes.individual.bottomNavigation

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.PorterDuff
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Menu
import android.view.View
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.widget.ViewPager2
import com.thehotelmedia.android.R
import com.thehotelmedia.android.Socket.SocketViewModel
import com.thehotelmedia.android.ViewModelFactory
import com.thehotelmedia.android.activity.BaseActivity
import com.thehotelmedia.android.activity.userTypes.forms.CreatePostActivity
import com.thehotelmedia.android.activity.userTypes.forms.createStory.CreateStoryActivity
import com.thehotelmedia.android.activity.userTypes.forms.reviewScreen.ReviewScreenActivity
import com.thehotelmedia.android.adapters.homes.IndividualViewPagerAdapter
import com.thehotelmedia.android.customClasses.CompassManager
import com.thehotelmedia.android.customClasses.Constants.DEFAULT_LAT
import com.thehotelmedia.android.customClasses.Constants.DEFAULT_LNG
import com.thehotelmedia.android.customClasses.Constants.FROM
import com.thehotelmedia.android.customClasses.Constants.business_type_individual
import com.thehotelmedia.android.customClasses.Constants.notification
import com.thehotelmedia.android.customClasses.CustomSnackBar
import com.thehotelmedia.android.customClasses.MessageStore
import com.thehotelmedia.android.customClasses.PreferenceManager
import com.thehotelmedia.android.customClasses.theme.ThemeHelper
import com.thehotelmedia.android.databinding.ActivityBottomNavigationIndividualMainBinding
import com.thehotelmedia.android.extensions.ChatDotUtil
import com.thehotelmedia.android.extensions.LocationHelper
import com.thehotelmedia.android.extensions.NotificationDotUtil
import com.thehotelmedia.android.extensions.blurTheView
import com.thehotelmedia.android.extensions.showToast
import com.thehotelmedia.android.repository.IndividualRepo
import com.thehotelmedia.android.viewModal.individualViewModal.IndividualViewModal

class BottomNavigationIndividualMainActivity : BaseActivity() {


    private lateinit var locationHelper: LocationHelper

    // Define the permission launcher as a class property
    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
                // Permissions granted, proceed to fetch location
                locationHelper.checkAndRequestLocation()
            } else {
                getWeatherAndAQI(DEFAULT_LAT,DEFAULT_LNG)
            }
        }

    private lateinit var compassManager: CompassManager
    private var currentDegree = 0f

    private lateinit var binding : ActivityBottomNavigationIndividualMainBinding
    private var TAG = "BottomNavigationIndividualMainActivity"
    var isRotated = false
    private lateinit var motionLayout: MotionLayout
    private val delayInMilliseconds = 300
    private var backPressedTime: Long = 0
    private lateinit var preferenceManager : PreferenceManager
    private val socketViewModel: SocketViewModel by viewModels()  // Initialize ViewModel here
    private var userName = ""

    private lateinit var individualViewModal: IndividualViewModal

    private var previouslySelectedItemIndex = 0



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityBottomNavigationIndividualMainBinding.inflate(layoutInflater)

        setContentView(binding.root)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isRotated) {
                    hidePlusBtn()
                } else {
                    handelBackClick()
                }
            }
        })




        initUI()

    }

    private fun checkLocationPermission() {
        // Initialize the LocationHelper with required callbacks
        locationHelper = LocationHelper(
            context = this,
            permissionLauncher = permissionLauncher,
            locationCallback = { latitude, longitude ->
                getWeatherAndAQI(latitude,longitude)
            },
            errorCallback = { errorMessage ->


            }
        )

        // Now check and request location permission when needed
        locationHelper.checkAndRequestLocation()
    }

    private fun getWeatherAndAQI(latitude: Double, longitude: Double) {

        println("adfasdklask    $latitude , $longitude")
        individualViewModal.getWeather(latitude,longitude)
        individualViewModal.getAQI(latitude,longitude)
    }


    private fun handelBackClick() {
        val selectedItemId: Int = binding.bottomNavigationView.selectedItemId
        if (selectedItemId != R.id.homeFrag) {
            binding.bottomNavigationView.selectedItemId = R.id.homeFrag
        } else {
            val currentTime = System.currentTimeMillis()
            if (currentTime - backPressedTime > 2000) {
                CustomSnackBar.showSnackBar(binding.root, MessageStore.pressBackAgainToExit(this))
                backPressedTime = currentTime
            } else {
                finishAffinity()
            }
        }
    }

    private fun initUI() {

        compassManager = CompassManager(this)
        // Observe changes in compass heading
        compassManager.degrees.observe(this) { degree ->
            val rotateAnim = RotateAnimation(
                currentDegree,
                -degree,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
            ).apply {
                duration = 300
                fillAfter = true
            }

            binding.compassIcon.startAnimation(rotateAnim)
            currentDegree = -degree
        }


        val individualRepo = IndividualRepo(this)
        individualViewModal = ViewModelProvider(this, ViewModelFactory(null, individualRepo, null))[IndividualViewModal::class.java]
        getNotificationStatus()

        preferenceManager = PreferenceManager.getInstance(applicationContext)
        preferenceManager.putString(PreferenceManager.Keys.BUSINESS_TYPE, business_type_individual)
        userName = preferenceManager.getString(PreferenceManager.Keys.USER_USER_NAME,"").toString()
        checkLocationPermission()

        motionLayout = binding.plusLayout
        // Disable the middle item in BottomNavigationView
        val menu: Menu = binding.bottomNavigationView.menu
        val itemToDisable = menu.findItem(R.id.addPlus)
        itemToDisable.isEnabled = false
//        binding.addIcon.setColorFilter(ContextCompat.getColor(this, R.color.icon_color_40), PorterDuff.Mode.SRC_IN)
        // Update chat dot with the unread message count
        ChatDotUtil.initializeAndUpdateChatDot(
            context = this,
            bottomNavigationView = binding.bottomNavigationView,
            preferenceManager = preferenceManager,
            count = 0)

        val from = intent.getStringExtra(FROM) ?: ""


        // Set up ViewPager2 with the adapter
        val adapter = IndividualViewPagerAdapter(this)
        binding.viewPager.adapter = adapter
        // Disable swipe if not needed
        binding.viewPager.isUserInputEnabled = false
        binding.viewPager.offscreenPageLimit = 4



        if (from == notification){
            previouslySelectedItemIndex = 3
            binding.viewPager.setCurrentItem(3, false)
            // When clearing unread messages
            ChatDotUtil.clearUnreadMessagesAndBroadcast(
                context = this,
                preferenceManager = preferenceManager
            )
            // Select the chat option in BottomNavigationView
            binding.bottomNavigationView.selectedItemId = R.id.chatFrag // Replace with your chat item ID

        }else{
            previouslySelectedItemIndex = 0
            binding.viewPager.setCurrentItem(0, false)
            binding.bottomNavigationView.selectedItemId = R.id.homeFrag
        }

        blurNavigationBar()



        binding.plusLayout.setOnClickListener {
            hidePlusBtn()
        }



        individualViewModal.getWeatherResult.observe(this){result->
            if (result != null){
                val weatherTemp = result.main.feels_like ?: 0.0
                val weatherType = result.weather[0].main ?: ""

                println("askdgjklsadj   $result")
                preferenceManager.putDouble(PreferenceManager.Keys.WEATHER_TEMP,weatherTemp)
                preferenceManager.putString(PreferenceManager.Keys.WEATHER_TYPE,weatherType.lowercase())
            }
        }
        individualViewModal.getAQIResult.observe(this){result->
            if (result != null){
                val aqi = result.list[0].main?.aqi ?: 0
                val pm25 = result.list[0].components?.pm25 ?: 0.0
                preferenceManager.putInt(PreferenceManager.Keys.AQI, aqi)
                preferenceManager.putDouble(PreferenceManager.Keys.AQI_PM25, pm25)
            }
        }




        binding.createPostLayout.setOnClickListener {
            val intent = Intent(this, CreatePostActivity::class.java)
            startActivity(intent)
        }

        binding.reviewLayout.setOnClickListener {
            val intent = Intent(this, ReviewScreenActivity::class.java)
            startActivity(intent)

        }

        binding.createStoryLayout.setOnClickListener {
            val intent = Intent(this, CreateStoryActivity::class.java)
            startActivity(intent)
        }


        binding.addLayout.setOnClickListener {
            if (!isRotated) {

                // Save the current selection and temporarily set an invalid ID
                binding.bottomNavigationView.menu.getItem(2).isChecked = false  // Uncheck all




                motionLayout.transitionToEnd()
//                binding.addIcon.setColorFilter(ContextCompat.getColor(this, R.color.blue))
//                binding.addIcon.animate().rotationBy(45f).setDuration(200).start()
                isRotated = true
//                binding.addDot.visibility = View.VISIBLE

                binding.plusLayout.visibility = View.VISIBLE
            } else {
                binding.bottomNavigationView.menu.getItem(previouslySelectedItemIndex).isChecked = false  // Uncheck all
                motionLayout.transitionToStart()
//                binding.addIcon.setColorFilter(ContextCompat.getColor(this, R.color.icon_color_40), PorterDuff.Mode.SRC_IN)
//                binding.addIcon.animate().rotationBy(-45f).setDuration(200).start()
                isRotated = false
//                binding.addDot.visibility = View.GONE
                Handler(Looper.getMainLooper()).postDelayed({
                    binding.plusLayout.visibility = View.INVISIBLE
                }, delayInMilliseconds.toLong())
            }

        }


// BottomNavigationView item selection listener
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.homeFrag -> {
                    previouslySelectedItemIndex = 0
                    hidePlusBtn()
                    val bundle = Bundle().apply {
                        putString("refresh", "Refresh")
                    }
                    supportFragmentManager.setFragmentResult("home_key", bundle)
                    binding.viewPager.setCurrentItem(0, false)
                    true  // Return true to indicate the event is handled
                }
                R.id.searchFrag -> {
                    previouslySelectedItemIndex = 1
                    hidePlusBtn()
                    binding.viewPager.setCurrentItem(1, false)
                    true
                }
                R.id.addPlus -> {
                    // Handle floating button separately (return false to not select this item)
                    // showAddItemDialog()
                    false
                }
                R.id.chatFrag -> {
                    previouslySelectedItemIndex = 3
                    hidePlusBtn()
                    binding.viewPager.setCurrentItem(3, false)
                    // Clear unread messages
                    ChatDotUtil.clearUnreadMessagesAndBroadcast(
                        context = this,
                        preferenceManager = preferenceManager
                    )
                    true
                }
                R.id.profileFrag -> {
                    previouslySelectedItemIndex = 4
                    hidePlusBtn()
                    binding.viewPager.setCurrentItem(4, false)
                    true
                }
                else -> false
            }
        }


        // Sync ViewPager2 with BottomNavigationView
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                binding.bottomNavigationView.menu.getItem(position).isChecked = true
                
                // Refresh chat fragment when Messages tab is selected (position 3)
                if (position == 3) {
                    // Clear unread messages
                    ChatDotUtil.clearUnreadMessagesAndBroadcast(
                        context = this@BottomNavigationIndividualMainActivity,
                        preferenceManager = preferenceManager
                    )
                    // Trigger data refresh in chat fragment
                    refreshChatFragment()
                }
            }
        })

        individualViewModal.notificationStatusResult.observe(this){result->
            if (result.status == true){
                val msg = result.message.toString()
                println("afklhsdkfh   ${result.data}")
                val data  = result.data
                val hasUnreadNotification = data?.notifications?.hasUnreadMessages ?: false
                val hasUnreadMessage = data?.messages?.hasUnreadMessages ?: false

                if (hasUnreadNotification){
                    showNotificationRedDot()
                }

                if (hasUnreadMessage){
                    showChatRedDot()
                }


            }
        }

    }
    private fun showNotificationRedDot() {
        NotificationDotUtil.setUnreadNotificationsAndBroadcast(this, preferenceManager)
    }

    private fun showChatRedDot() {
        ChatDotUtil.setUnreadMessagesAndBroadcast(this, preferenceManager, 1)
    }

    private fun getNotificationStatus() {
        individualViewModal.getNotificationStatus()
    }

    override fun onResume() {
        println("SocketManager    connectSocket(  $userName  )  ")
        socketViewModel.connectSocket(userName)
        super.onResume()
    }

    override fun onPause() {
//        socketViewModel.disconnectSocket()
        super.onPause()
    }

    override fun onDestroy() {
        // Unregister the chat dot receiver
        compassManager.stop()
        ChatDotUtil.unregisterReceiver(this)
        socketViewModel.disconnectSocket()
        super.onDestroy()
    }

    private fun blurNavigationBar() {
        blurTheView(binding.blurView)
    }

    private fun refreshChatFragment() {
        // Get the chat fragment and trigger refresh
        try {
            // ViewPager2 uses "f" + position as fragment tag
            val fragmentTag = "f${3}"
            val fragment = supportFragmentManager.findFragmentByTag(fragmentTag)
            if (fragment is com.thehotelmedia.android.fragments.userTypes.individual.bottomNavigation.IndividualChatFragment) {
                // Post with a small delay to ensure fragment is fully visible
                Handler(Looper.getMainLooper()).postDelayed({
                    if (fragment.isAdded && !fragment.isHidden) {
                        fragment.refreshDataIfNeeded()
                    }
                }, 150)
            }
        } catch (e: Exception) {
            // Handle exception silently
        }
    }

    private fun hidePlusBtn() {
        // First, set the dot to visible but make it transparent initially
        if (isRotated) {
            binding.bottomNavigationView.menu.getItem(previouslySelectedItemIndex).isChecked = false
//            binding.addIcon.setColorFilter(ContextCompat.getColor(this, R.color.icon_color_40), PorterDuff.Mode.SRC_IN)
            motionLayout.transitionToStart()
//            binding.addIcon.animate().rotationBy(-45f).setDuration(200).start()
            isRotated = false
//            binding.addDot.visibility = View.GONE
            Handler(Looper.getMainLooper()).postDelayed({
                binding.plusLayout.visibility = View.INVISIBLE
            }, delayInMilliseconds.toLong())
        }
    }


}
