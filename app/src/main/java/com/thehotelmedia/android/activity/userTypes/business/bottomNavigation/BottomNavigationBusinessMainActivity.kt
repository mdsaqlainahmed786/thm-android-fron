package com.thehotelmedia.android.activity.userTypes.business.bottomNavigation

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
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.ViewModelProvider
import com.google.gson.Gson
import com.thehotelmedia.android.R
import com.thehotelmedia.android.Socket.SocketViewModel
import com.thehotelmedia.android.ViewModelFactory
import com.thehotelmedia.android.activity.BaseActivity
import com.thehotelmedia.android.activity.stories.ViewStoriesActivity
import com.thehotelmedia.android.activity.userTypes.forms.CreatePostActivity
import com.thehotelmedia.android.activity.userTypes.forms.createEvent.CreateEventActivity
import com.thehotelmedia.android.activity.userTypes.forms.createStory.CreateStoryActivity
import com.thehotelmedia.android.adapters.homes.BusinessViewPagerAdapter
import com.thehotelmedia.android.customClasses.CompassManager
import com.thehotelmedia.android.customClasses.Constants.DEFAULT_LAT
import com.thehotelmedia.android.customClasses.Constants.DEFAULT_LNG
import com.thehotelmedia.android.customClasses.Constants.FROM
import com.thehotelmedia.android.customClasses.Constants.business_type_business
import com.thehotelmedia.android.customClasses.Constants.notification
import com.thehotelmedia.android.customClasses.CustomSnackBar
import com.thehotelmedia.android.customClasses.MessageStore
import com.thehotelmedia.android.customClasses.PreferenceManager
import com.thehotelmedia.android.databinding.ActivityBottomNavigationBusinessMainBinding
import com.thehotelmedia.android.extensions.ChatDotUtil
import com.thehotelmedia.android.extensions.LocationHelper
import com.thehotelmedia.android.extensions.NotificationDotUtil
import com.thehotelmedia.android.extensions.blurTheView
import com.thehotelmedia.android.extensions.isRecentPost
import com.thehotelmedia.android.extensions.showToast
import com.thehotelmedia.android.modals.Stories.ProfilePic
import com.thehotelmedia.android.modals.Stories.Stories
import com.thehotelmedia.android.modals.Stories.StoriesData
import com.thehotelmedia.android.modals.Stories.StoriesRef
import com.thehotelmedia.android.repository.IndividualRepo
import com.thehotelmedia.android.viewModal.individualViewModal.IndividualViewModal
import kotlinx.coroutines.launch

class BottomNavigationBusinessMainActivity : BaseActivity() {


    private lateinit var locationHelper: LocationHelper

    // Define the permission launcher as a class property
    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
                // Permissions granted, proceed to fetch location
                locationHelper.checkAndRequestLocation()
            } else {
                getWeatherAndAQI(DEFAULT_LAT, DEFAULT_LNG)
            }
        }
    private lateinit var binding: ActivityBottomNavigationBusinessMainBinding
    private var TAG = "BottomNavigationIndividualMainActivity"
    var isRotated = false
    private lateinit var motionLayout: MotionLayout
    private val delayInMilliseconds = 300
    private var backPressedTime: Long = 0
    private var userName = ""
    private lateinit var preferenceManager : PreferenceManager

    private val socketViewModel: SocketViewModel by viewModels()  // Initialize ViewModel here

    private lateinit var individualViewModal: IndividualViewModal
    private var previouslySelectedItemIndex = 0

    private lateinit var compassManager: CompassManager
    private var currentDegree = 0f


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBottomNavigationBusinessMainBinding.inflate(layoutInflater)
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationDeepLink(intent)
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

        preferenceManager.putString(PreferenceManager.Keys.BUSINESS_TYPE, business_type_business)
        userName = preferenceManager.getString(PreferenceManager.Keys.USER_USER_NAME,"").toString()
        checkLocationPermission()

        // Handle push deep links (route-driven)
        handleNotificationDeepLink(intent)



        motionLayout = binding.plusLayout
        // Disable the middle item in BottomNavigationView
        val menu: Menu = binding.bottomNavigationView.menu
        val itemToDisable = menu.findItem(R.id.addPlus)
        itemToDisable.isEnabled = false
//        binding.addIcon.setColorFilter(ContextCompat.getColor(this, R.color.icon_color_40), PorterDuff.Mode.SRC_IN)
//        setCurrentFragment(IndividualHomeFragment())
        // Update chat dot with the unread message count
        ChatDotUtil.initializeAndUpdateChatDot(
            context = this,
            bottomNavigationView = binding.bottomNavigationView,
            preferenceManager = preferenceManager,
            count = 0)

        val from = intent.getStringExtra(FROM) ?: ""

//        if (from == notification){
//            setCurrentFragment(IndividualChatFragment())
//            // When clearing unread messages
//            ChatDotUtil.clearUnreadMessagesAndBroadcast(
//                context = this,
//                preferenceManager = preferenceManager
//            )
//            // Select the chat option in BottomNavigationView
//            binding.bottomNavigationView.selectedItemId = R.id.chatFrag // Replace with your chat item ID
//
//        }else{
//            setCurrentFragment(IndividualHomeFragment())
//            binding.bottomNavigationView.selectedItemId = R.id.homeFrag
//        }

        // Set up ViewPager2 with the adapter
        val adapter = BusinessViewPagerAdapter(this)
        binding.viewPager.adapter = adapter
        // Disable swipe if not needed
        binding.viewPager.isUserInputEnabled = false
        binding.viewPager.offscreenPageLimit = 4

        if (from == notification){
//            setCurrentFragment(IndividualChatFragment())
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
//            setCurrentFragment(IndividualHomeFragment())
            previouslySelectedItemIndex = 0
            binding.viewPager.setCurrentItem(0, false)
            binding.bottomNavigationView.selectedItemId = R.id.homeFrag
        }


        blurNavigationBar()



        binding.plusLayout.setOnClickListener {
            hidePlusBtn()
        }

        binding.createPostLayout.setOnClickListener {
            val intent = Intent(this, CreatePostActivity::class.java)
            startActivity(intent)
        }

        binding.eventLayout.setOnClickListener {
            val intent = Intent(this, CreateEventActivity::class.java)
            startActivity(intent)
        }

        binding.createStoryLayout.setOnClickListener {
            val intent = Intent(this, CreateStoryActivity::class.java)
            startActivity(intent)
        }


        individualViewModal.getWeatherResult.observe(this){result->
            if (result != null){
                val weatherTemp = result.main.feels_like ?: 0.0
                val weatherType = result.weather[0].main ?: ""

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

//        binding.bottomNavigationView.setOnItemSelectedListener(NavigationBarView.OnItemSelectedListener { item ->
//            when (item.itemId) {
//                R.id.homeFrag -> {
//                    setCurrentFragment(IndividualHomeFragment())
//                    return@OnItemSelectedListener true
//                }
//                R.id.insiteFrag -> {
//                    setCurrentFragment(BusinessInsightFragment())
//                    return@OnItemSelectedListener true
//                }
//                R.id.addPlus -> {
//
////                    setCurrentFragment(ExploreFragment())
//                    return@OnItemSelectedListener true
//                }
//                R.id.chatFrag -> {
//                    setCurrentFragment(IndividualChatFragment())
//                    // When clearing unread messages
//                    ChatDotUtil.clearUnreadMessagesAndBroadcast(
//                        context = this,
//                        preferenceManager = preferenceManager
//                    )
//                    return@OnItemSelectedListener true
//                }
//                R.id.profileFrag -> {
//                    setCurrentFragment(IndividualProfileFragment())
//                    return@OnItemSelectedListener true
//                }
//                else -> false
//            }
//        })

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
                R.id.insiteFrag -> {
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

    private fun getWeatherAndAQI(latitude: Double, longitude: Double) {
        individualViewModal.getWeather(latitude,longitude)
        individualViewModal.getAQI(latitude,longitude)
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
        val userID = preferenceManager.getString(PreferenceManager.Keys.USER_ID, "").orEmpty()
        socketViewModel.connectSocket(userName, userID)
        super.onResume()
    }

    override fun onPause() {
//        socketViewModel.disconnectSocket()
        super.onPause()
    }

    override fun onDestroy() {
        compassManager.stop() // Stop compass updates
        // Unregister the chat dot receiver
        ChatDotUtil.unregisterReceiver(this)
        socketViewModel.disconnectSocket()
        super.onDestroy()
    }

    private fun blurNavigationBar() {

        blurTheView(binding.blurView)
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

    private fun handleNotificationDeepLink(intent: Intent?) {
        if (intent == null) return

        val route = intent.getStringExtra("route")?.takeIf { it.isNotBlank() }
            ?: intent.getStringExtra("screen")?.takeIf { it.isNotBlank() }

        if (route != "story_detail") return

        val storyId = intent.getStringExtra("storyID")?.takeIf { it.isNotBlank() }
            ?: intent.getStringExtra("storyId")?.takeIf { it.isNotBlank() }

        if (storyId.isNullOrBlank()) return

        // Consume extras to avoid re-triggering on rotation/resume
        intent.removeExtra("route")
        intent.removeExtra("screen")
        intent.removeExtra("storyID")
        intent.removeExtra("storyId")

        openStoryById(storyId)
    }

    private fun openStoryById(storyId: String) {
        lifecycleScope.launch {
            try {
                val repo = IndividualRepo(this@BottomNavigationBusinessMainActivity)
                val response = repo.getStories(pageNumber = 1, documentLimit = 50)
                if (!response.isSuccessful) {
                    showToast("Story no longer available")
                    return@launch
                }

                val storiesData = response.body()?.storiesData
                val storyOwnerBundle = findStoryOwnerBundle(storiesData, storyId)
                if (storyOwnerBundle == null) {
                    showToast("Story no longer available")
                    return@launch
                }

                val jsonString = Gson().toJson(listOf(storyOwnerBundle))
                val viewIntent = Intent(this@BottomNavigationBusinessMainActivity, ViewStoriesActivity::class.java).apply {
                    putExtra("StoriesJson", jsonString)
                }
                startActivity(viewIntent)
            } catch (e: Exception) {
                showToast("Story no longer available")
            }
        }
    }

    /**
     * Locate a story by ID in the stories payload and return a single-user Stories bundle
     * compatible with ViewStoriesActivity ("StoriesJson").
     */
    private fun findStoryOwnerBundle(storiesData: StoriesData?, storyId: String): Stories? {
        if (storiesData == null) return null

        // 1) Check myStories (list of StoriesRef only)
        val myStoryRef: StoriesRef? = storiesData.myStories.firstOrNull { it.Id == storyId }
        if (myStoryRef != null) {
            val createdAt = myStoryRef.createdAt ?: ""
            if (createdAt.isNotEmpty() && !isRecentPost(createdAt)) return null

            val userName = preferenceManager.getString(PreferenceManager.Keys.USER_USER_NAME, "").orEmpty()
            val fullName = preferenceManager.getString(PreferenceManager.Keys.USER_FULL_NAME, "").orEmpty()
            val smallProfilePic = preferenceManager.getString(PreferenceManager.Keys.USER_SMALL_PROFILE_PIC, "").orEmpty()
            val mediumProfilePic = preferenceManager.getString(PreferenceManager.Keys.USER_MEDIUM_PROFILE_PIC, "").orEmpty()
            val largeProfilePic = preferenceManager.getString(PreferenceManager.Keys.USER_LARGE_PROFILE_PIC, "").orEmpty()

            return Stories(
                id = preferenceManager.getString(PreferenceManager.Keys.USER_ID, "").orEmpty(),
                accountType = "business",
                username = userName,
                name = fullName,
                profilePic = ProfilePic(small = smallProfilePic, medium = mediumProfilePic, large = largeProfilePic),
                businessProfileRef = null,
                storiesRef = arrayListOf(myStoryRef),
                seenByMe = null
            )
        }

        // 2) Check other users' stories (already bundled)
        for (userStories in storiesData.stories) {
            val storyRef = userStories.storiesRef.firstOrNull { it.Id == storyId } ?: continue
            val createdAt = storyRef.createdAt ?: ""
            if (createdAt.isNotEmpty() && !isRecentPost(createdAt)) return null

            return userStories.copy(storiesRef = arrayListOf(storyRef))
        }

        return null
    }

//    private fun setCurrentFragment(fragment: Fragment) {
//        hidePlusBtn()
//
//        // Clear back stack before switching to a new fragment
//        supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
//
//        val transaction = this.supportFragmentManager.beginTransaction()
//        transaction.replace(R.id.frameLayout, fragment)
//        // Don't add the transaction to the back stack so previous fragments are not kept alive
//        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
//        transaction.commit()
//
//    }
}