package com.thehotelmedia.android.activity

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.google.android.material.tabs.TabLayout
import com.google.gson.Gson
import com.thehotelmedia.android.BuildConfig
import com.thehotelmedia.android.R
import com.thehotelmedia.android.ViewModelFactory
import com.thehotelmedia.android.activity.booking.AddDetailsActivity
import com.thehotelmedia.android.activity.booking.BookBanquetActivity
import com.thehotelmedia.android.activity.booking.BookTableActivity
import com.thehotelmedia.android.activity.userTypes.forms.reviewScreen.ReviewScreenActivity
import com.thehotelmedia.android.activity.userTypes.profile.FollowerFollowingActivity
import com.thehotelmedia.android.adapters.userTypes.individual.profile.AmenitiesAdapter
import com.thehotelmedia.android.adapters.userTypes.individual.profile.QuestionLinesAdapter
import com.thehotelmedia.android.bottomSheets.BlockUserBottomSheetFragment
import com.thehotelmedia.android.bottomSheets.ReportBottomSheetFragment
import com.thehotelmedia.android.customClasses.ColorFilterTransformation
import com.thehotelmedia.android.customClasses.Constants.OFFICIAL
import com.thehotelmedia.android.customClasses.Constants.URL_PATTERN
import com.thehotelmedia.android.customClasses.Constants.business_type_business
import com.thehotelmedia.android.customClasses.Constants.business_type_individual
import com.thehotelmedia.android.customClasses.CustomProgressBar
import com.thehotelmedia.android.customClasses.CustomSnackBar
import com.thehotelmedia.android.customClasses.ImageDialog
import com.thehotelmedia.android.customClasses.PreferenceManager
import com.thehotelmedia.android.databinding.ActivityBusinessProfileDetailsBinding
import com.thehotelmedia.android.extensions.EncryptionHelper
import com.thehotelmedia.android.extensions.blurTheView
import com.thehotelmedia.android.extensions.callPhoneNumber
import com.thehotelmedia.android.extensions.capitalizeFirstLetter
import com.thehotelmedia.android.extensions.openGoogleMaps
import com.thehotelmedia.android.extensions.setRatingWithStarWithoutBracket
import com.thehotelmedia.android.extensions.shareProfileWithDeepLink
import com.thehotelmedia.android.extensions.toAQI
import com.thehotelmedia.android.fragments.userTypes.individual.profile.ProfileMenuFragment
import com.thehotelmedia.android.fragments.userTypes.individual.profile.ProfilePhotosFragment
import com.thehotelmedia.android.fragments.userTypes.individual.profile.ProfilePostsFragment
import com.thehotelmedia.android.fragments.userTypes.individual.profile.ProfileReviewsFragment
import com.thehotelmedia.android.fragments.userTypes.individual.profile.ProfileVideosFragment
import com.thehotelmedia.android.modals.checkinData.checkInData.ReviewQuestions
import com.thehotelmedia.android.modals.profileData.profile.AmenitiesRef
import com.thehotelmedia.android.modals.profileData.profile.BusinessAnswerRef
import com.thehotelmedia.android.modals.userProfile.UserProfileModel
import com.thehotelmedia.android.repository.IndividualRepo
import com.thehotelmedia.android.viewModal.individualViewModal.IndividualViewModal
import kotlin.math.roundToInt

class BusinessProfileDetailsActivity : BaseActivity() , BlockUserBottomSheetFragment.BottomSheetListener {

    private lateinit var binding: ActivityBusinessProfileDetailsBinding
    private lateinit var tab: TabLayout

    private lateinit var individualViewModal: IndividualViewModal
    private lateinit var progressBar: CustomProgressBar
    private var accountType = ""
    private var myAccountType = ""
    private var userId = ""
    private var businessProfileId = ""
    private var outerUserId = ""
    private var ownerUserId = ""
    private var sharedByUserId = ""
    private var placeID = ""
    private var bio = ""
    private var from = ""
    private var username = ""
    private var websiteLink = ""
    private var isConnected : Boolean = false
    private var isRequested : Boolean = false
    private var isBlocked : Boolean = false
    private lateinit var tabTitles: Array<String>
    private lateinit var tabIcons: Array<Int>
    private lateinit var amenitiesRef: ArrayList<AmenitiesRef>
    private lateinit var answerAmenitiesRef: ArrayList<BusinessAnswerRef>
    private var userMediumProfilePic = ""
    private var userLargeProfilePic = ""
    private var businessName = ""
    private var businessIcon = ""
    private var fullAddress = ""
    private var userFullName = ""
    private var rating = 0.0
    private var lat = 0.0
    private var lng = 0.0
    private val handler = Handler(Looper.getMainLooper())
    private var phoneNumber = ""
    private var role = ""
    private lateinit var preferenceManager : PreferenceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBusinessProfileDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
//        userId = intent.getStringExtra("USER_ID") ?: ""
//        outerUserId = intent.getStringExtra("USER_ID") ?: ""

        val individualRepo = IndividualRepo(this)
        individualViewModal = ViewModelProvider(this, ViewModelFactory(null,individualRepo,null))[IndividualViewModal::class.java]
        progressBar = CustomProgressBar(this)

        if (intent.action == Intent.ACTION_VIEW) {
            val uri: Uri? = intent.data
            uri?.let {
                val id = it.getQueryParameter("id").toString()
                val userID = it.getQueryParameter("userID").toString()

                // Extract the encrypted values directly
                val encryptedId = it.getQueryParameter("id").toString()
                val encryptedUserID = it.getQueryParameter("userID").toString()

                individualViewModal.shareProfile(encryptedId,encryptedUserID)

                // Decrypt the values
                val decryptedId = EncryptionHelper.decrypt(encryptedId)
                val decryptedUserID = EncryptionHelper.decrypt(encryptedUserID)

                // Now, you have the category ID and name, you can use them to populate the UI or perform any necessary actions
                userId = decryptedId
                outerUserId = decryptedId
                sharedByUserId = decryptedUserID
            }
        }else{
            userId = intent.getStringExtra("USER_ID") ?: ""
            outerUserId = intent.getStringExtra("USER_ID") ?: ""
        }


        initUI()
    }

    private fun initUI() {

        blurTheView(binding.callBlurView)
        blurTheView(binding.bookingBlurView)
        blurTheView(binding.bookTableBlurView)
        blurTheView(binding.bookBanquetBlurView)
        blurTheView(binding.locationBlurView)


        preferenceManager = PreferenceManager.getInstance(this)
        myAccountType = preferenceManager.getString(PreferenceManager.Keys.BUSINESS_TYPE, "").toString()
        ownerUserId = preferenceManager.getString(PreferenceManager.Keys.USER_ID, "").toString()

//        if (userId == ownerUserId){
//            if (myAccountType == business_type_individual){
//                val intent = Intent(this, BottomNavigationIndividualMainActivity::class.java)
//                startActivity(intent)
//                finish()
//            }else{
//                val intent = Intent(this, BottomNavigationBusinessMainActivity::class.java)
//                startActivity(intent)
//                finish()
//            }
//        }


        if (myAccountType == business_type_individual){
            binding.writeReviewBtn.visibility = View.GONE
        }else{
            binding.writeReviewBtn.visibility = View.VISIBLE
        }


        getUserProfile()

        binding.backBtn.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.bookNowBtn.setOnClickListener {
                val intent = Intent(this, AddDetailsActivity::class.java)
                intent.putExtra("KEY_BUSINESS_PROFILE_ID", businessProfileId)
                intent.putExtra("KEY_USER_LARGE_PROFILE_PIC", userLargeProfilePic)
                intent.putExtra("KEY_USER_FULL_NAME", userFullName)
                intent.putExtra("KEY_BUSINESS_NAME", businessName)
                intent.putExtra("KEY_BUSINESS_ICON", businessIcon)
                intent.putExtra("KEY_FULL_ADDRESS", fullAddress)
                intent.putExtra("KEY_RATING", rating)
                startActivity(intent)
        }
        binding.bookTableBtn.setOnClickListener {
                val intent = Intent(this, BookTableActivity::class.java)
                intent.putExtra("KEY_BUSINESS_PROFILE_ID", businessProfileId)
                intent.putExtra("KEY_USER_LARGE_PROFILE_PIC", userLargeProfilePic)
                intent.putExtra("KEY_USER_FULL_NAME", userFullName)
                intent.putExtra("KEY_BUSINESS_NAME", businessName)
                intent.putExtra("KEY_BUSINESS_ICON", businessIcon)
                intent.putExtra("KEY_FULL_ADDRESS", fullAddress)
                intent.putExtra("KEY_RATING", rating)
                startActivity(intent)
        }
        binding.bookBanquetBtn.setOnClickListener {
                val intent = Intent(this, BookBanquetActivity::class.java)
                intent.putExtra("KEY_BUSINESS_PROFILE_ID", businessProfileId)
                intent.putExtra("KEY_USER_LARGE_PROFILE_PIC", userLargeProfilePic)
                intent.putExtra("KEY_USER_FULL_NAME", userFullName)
                intent.putExtra("KEY_BUSINESS_NAME", businessName)
                intent.putExtra("KEY_BUSINESS_ICON", businessIcon)
                intent.putExtra("KEY_FULL_ADDRESS", fullAddress)
                intent.putExtra("KEY_RATING", rating)
                startActivity(intent)
        }

        binding.callBtn.setOnClickListener {
            this.callPhoneNumber(phoneNumber)
        }

        binding.locationBtn.setOnClickListener {
            this.openGoogleMaps(lat, lng)
        }


        binding.profileIv.setOnClickListener {
            val imageDialog = ImageDialog(this)
            imageDialog.showImage(userLargeProfilePic)
        }

        binding.menuBtn.isClickable = true
        binding.menuBtn.isFocusable = true
        binding.menuBtn.setOnClickListener { view ->
            showMenuDialog(view)
        }

        binding.writeReviewBtn.setOnClickListener {
            getCheckInData()
        }

        binding.followerBtnLayout.setOnClickListener {
            if (role != OFFICIAL){
                moveToFollowerFollowingActivity("Follower")
            }
        }

        binding.followingBtnLayout.setOnClickListener {
            if (role != OFFICIAL){
                moveToFollowerFollowingActivity("Following")
            }
        }

        binding.followBtn.setOnClickListener {
            if(!isConnected && isRequested){
                individualViewModal.unFollowUser(outerUserId)
            }else{
                individualViewModal.followUser(outerUserId)
            }
        }

        binding.unFollowBtn.setOnClickListener {
            individualViewModal.unFollowUser(outerUserId)
        }

        binding.unblockBtn.setOnClickListener {
            blockUser()
        }

        binding.messageBtn.setOnClickListener {
            val intent = Intent(this, InboxScreenActivity::class.java)
            intent.putExtra("NAME",userFullName)
            intent.putExtra("USER_NAME",username)
            intent.putExtra("PROFILE_PIC",userMediumProfilePic)
            startActivity(intent)
        }

        binding.websiteBtn.setOnClickListener {
            if (!websiteLink.matches(URL_PATTERN)) {
                CustomSnackBar.showSnackBar(binding.root,getString(R.string.invalid_url))
            }else{
                individualViewModal.visitWebsite(businessProfileId)
                openAppOrBrowser(this,"com.android.chrome",websiteLink)
            }
        }




        individualViewModal.userProfileByIdResult.observe(this){result->
            if (result?.status == true){
                handelProfileData(result)
            }else{
                val msg = result?.message?.toString() ?: "Failed to load profile"
                CustomSnackBar.showSnackBar(binding.root,msg)
                progressBar.hide()
            }
        }
        individualViewModal.followUserResult.observe(this){result->
            if (result.status == true){
                val status = result.data?.status
                if (status == "pending"){
                    isConnected = false
                    isRequested = true
                    binding.followTv.text = getString(R.string.requested)
                    binding.followBtn.isEnabled = true
                    binding.followBtn.visibility = View.VISIBLE
                    binding.unFollowBtn.visibility = View.GONE
                    notifyPostsFragmentFollowState()
                }else{
                    binding.followTv.text = getString(R.string.following)
                    isConnected = true
                    isRequested = false
                    binding.followBtn.isEnabled = false
                    binding.followBtn.visibility = View.GONE
                    binding.unFollowBtn.visibility = View.VISIBLE
                    notifyPostsFragmentFollowState()
                }
            }else{
                val msg = result.message.toString()
                CustomSnackBar.showSnackBar(binding.root,msg)
            }
        }

        individualViewModal.unFollowUserResult.observe(this){result->
            if (result.status == true){
                binding.followTv.text = getString(R.string.follow)
                isConnected = false
                isRequested = false
                binding.followBtn.isEnabled = true

                binding.followBtn.visibility = View.VISIBLE
                binding.unFollowBtn.visibility = View.GONE
                notifyPostsFragmentFollowState()

            }else{
                val msg = result.message.toString()
                CustomSnackBar.showSnackBar(binding.root,msg)
            }
        }



        individualViewModal.getCheckInDataResult.observe(this){result->
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
                moveToReviewScreen(businessName,street, city, state, zipCode, country, lat, long, coverImage, profileImage,reviewQuestion,businessId,typeOfId)
            }else{
                val msg = result.message
                Toast.makeText(this,msg, Toast.LENGTH_SHORT).show()
            }
        }


        individualViewModal.loading.observe(this){
            if (it == true){
                progressBar.show()
            }else{
                progressBar.hide()
            }
        }

        individualViewModal.toast.observe(this){
            CustomSnackBar.showSnackBar(binding.root,it)
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)  // Cleanup to avoid memory leaks
    }

    private fun moveToReviewScreen(
        businessName: String,
        street: String,
        city: String,
        state: String,
        zipCode: String,
        country: String,
        lat: Double,
        long: Double,
        coverImage: String,
        profileImage: String,
        reviewQuestion: java.util.ArrayList<ReviewQuestions>?,
        businessId: String,
        typeOfId: String
    ) {

        val gson = Gson()
        val reviewQuestionJson = gson.toJson(reviewQuestion)
        val intent = Intent(this, ReviewScreenActivity::class.java)
        intent.putExtra("FROM", "UserProfileDetailScreen")
        intent.putExtra("PLACE_ADDRESS", "$city, $state, $country, $zipCode")
        intent.putExtra("PLACE_NAME", businessName)
        intent.putExtra("PLACE_ID", placeID)
        intent.putExtra("STREET", street)
        intent.putExtra("CITY", city)
        intent.putExtra("STATE", state)
        intent.putExtra("ZIPCODE", zipCode)
        intent.putExtra("COUNTRY", country)
        intent.putExtra("LAT", lat)
        intent.putExtra("LNG", long)
        intent.putExtra("COVER_IMAGE", coverImage)
        intent.putExtra("PROFILE_PIC", profileImage)
        intent.putExtra("REVIEW_QUESTION", reviewQuestionJson)
        intent.putExtra("BUSINESS_ID", businessId)
        intent.putExtra("TYPE_OF_ID", typeOfId)
        startActivity(intent)
    }


    private fun handelProfileData(result: UserProfileModel?) {
        val privateAcc = result?.data?.privateAccount ?: false

        isConnected = result?.data?.isConnected ?: false
        isRequested = result?.data?.isRequested ?: false
        notifyPostsFragmentFollowState()
        val bookingType = result?.data?.booking ?: ""

        val businessTypeRef = result?.data?.businessProfileRef?.businessTypeRef
        businessName = businessTypeRef?.name ?: ""
        val isHotel = businessName.equals(getString(R.string.hotel), ignoreCase = true)
        // More robust check: check if name contains "restaurant" or matches exactly
        val isRestaurant = businessName.equals(getString(R.string.restaurant), ignoreCase = true) ||
                businessName.lowercase().trim().contains("restaurant")

        // Show menu CTA only for restaurants (not hotels)
        // Check if viewing own profile: compare logged-in user ID with profile owner's ID
        val profileOwnerId = result?.data?.Id ?: outerUserId
        val isOwnProfile = ownerUserId == profileOwnerId || ownerUserId == outerUserId
        binding.menuCtaLayout.visibility = if (isRestaurant) View.VISIBLE else View.GONE
        
        // Set button text based on whether viewing own profile or others
        if (isRestaurant) {
            binding.viewMenuBtn.text = if (isOwnProfile) getString(R.string.view_your_menu) else getString(R.string.view_menu)
        }

        when (bookingType) {
            "booking" -> {
                binding.bookNowBtn.visibility = View.VISIBLE
                binding.bookTableBtn.visibility = View.GONE
                binding.bookBanquetBtn.visibility = View.GONE
                if (isHotel) {
                    binding.bookingTv.text = getString(R.string.book_room)
                }
            }
            "book-table" -> {
                if (isHotel) {
                    // For hotels, show "Book Room" button instead of "Book Table"
                    binding.bookNowBtn.visibility = View.VISIBLE
                    binding.bookTableBtn.visibility = View.GONE
                    binding.bookBanquetBtn.visibility = View.GONE
                    binding.bookingTv.text = getString(R.string.book_room)
                } else {
                    binding.bookNowBtn.visibility = View.GONE
                    binding.bookTableBtn.visibility = View.VISIBLE
                    binding.bookBanquetBtn.visibility = View.GONE
                }
            }
            "book-banquet" -> {
                binding.bookNowBtn.visibility = View.GONE
                binding.bookTableBtn.visibility = View.GONE
                binding.bookBanquetBtn.visibility = View.VISIBLE
            }
            else -> {
                if (isHotel) {
                    // For hotels, show "Book Room" button by default
                    binding.bookNowBtn.visibility = View.VISIBLE
                    binding.bookTableBtn.visibility = View.GONE
                    binding.bookBanquetBtn.visibility = View.GONE
                    binding.bookingTv.text = getString(R.string.book_room)
                } else {
                    binding.bookNowBtn.visibility = View.GONE
                    binding.bookTableBtn.visibility = View.VISIBLE
                    binding.bookBanquetBtn.visibility = View.GONE
                }
            }
        }


        if (privateAcc){
//            userId = ""
            if (!isConnected){
                userId = ""
            }
        }

        businessProfileId = result?.data?.businessProfileID ?: ""
        accountType = result?.data?.accountType?.capitalizeFirstLetter() ?: ""
        setupTabBar()

        val postCount = result?.data?.posts
        placeID = result?.data?.businessProfileRef?.placeID ?: ""

        val followerCount = result?.data?.follower
        val followingCount = result?.data?.following

        val profilePic = result?.data?.profilePic
        username = result?.data?.username.toString()

        businessIcon = businessTypeRef?.icon ?: ""

        val dialCode = result?.data?.businessProfileRef?.dialCode ?: ""
        val number = result?.data?.businessProfileRef?.phoneNumber ?: ""
        phoneNumber = "$dialCode$number"

        binding.businessNameTv.text = businessName
        val address =  result?.data?.businessProfileRef?.address
        val street = address?.street ?: ""
        val city = address?.city ?: ""
        val state = address?.state ?: ""
        val country = address?.country ?: ""
        val zipCode = address?.zipCode ?: ""
        fullAddress = "$street, $city, $state, $country, $zipCode"

        if (country.isNotEmpty()){
        binding.fullAddressTv.text = fullAddress

        // Setup View Menu button click (restaurant only)
        binding.viewMenuBtn.setOnClickListener {
            if (!isRestaurant) return@setOnClickListener
            if (businessProfileId.isNotEmpty()) {
                val intent = Intent(this, MenuViewerActivity::class.java).apply {
                    putExtra("BUSINESS_PROFILE_ID", businessProfileId)
                    putExtra("INITIAL_INDEX", 0)
                }
                startActivity(intent)
            } else {
                CustomSnackBar.showSnackBar(binding.root, getString(R.string.no_menu_available))
            }
        }
            binding.fullAddressTv.visibility = View.VISIBLE
        }else{
            binding.fullAddressTv.visibility = View.GONE
        }

        Glide.with(this).load(businessIcon)
            .placeholder(R.drawable.ic_hotel)
            .transform(ColorFilterTransformation(ContextCompat.getColor(this, R.color.text_color)))
            .into(binding.businessIconIv)

        from = if (isBlocked){
            "BlockScreen"
        }else{
            ""
        }

//        if (!isConnected && !isRequested){
//            binding.followTv.text = getString(R.string.follow)
//            binding.followBtn.isEnabled = true
//            binding.followBtn.visibility = View.VISIBLE
//            binding.unFollowBtn.visibility = View.GONE
//
//        }else if(!isConnected && isRequested){
//            binding.followTv.text = getString(R.string.requested)
//            binding.followBtn.isEnabled = true
//            binding.followBtn.visibility = View.VISIBLE
//            binding.unFollowBtn.visibility = View.GONE
//
//        }else if(isConnected && !isRequested){
//            binding.followTv.text = getString(R.string.following)
//            binding.followBtn.isEnabled = false
//            binding.followBtn.visibility = View.GONE
//            binding.unFollowBtn.visibility = View.VISIBLE
//
//        }


        binding.followBtn.isEnabled = true
        binding.followBtn.visibility = View.VISIBLE
        binding.unFollowBtn.visibility = View.GONE

        when {
            !isConnected && !isRequested -> binding.followTv.text = getString(R.string.follow)
            !isConnected && isRequested -> binding.followTv.text = getString(R.string.requested)
            isConnected && !isRequested -> {
                binding.followTv.text = getString(R.string.following)
                binding.followBtn.isEnabled = false
                binding.followBtn.visibility = View.GONE
                binding.unFollowBtn.visibility = View.VISIBLE
            }
        }

        if (from == "BlockScreen"){
            binding.menuBtn.visibility = View.GONE
            binding.followLayout.visibility = View.GONE
            binding.unblockLayout.visibility = View.VISIBLE
        }else{
            binding.menuBtn.visibility = View.VISIBLE
            binding.followLayout.visibility = View.VISIBLE
            binding.unblockLayout.visibility = View.GONE
        }

        if (accountType == business_type_individual){
            binding.profileProgress.progress = 0
            bio = result?.data?.bio.toString()
            if (bio.isNotEmpty()){
                val questionAdapter = QuestionLinesAdapter(this,bio)
                binding.questionRv.adapter = questionAdapter
            }
            userFullName = result?.data?.name.toString()
//            dialCode = result.data?.dialCode.toString()
//            phoneNumber = result.data?.phoneNumber.toString()
//            userSmallProfilePic = profilePic?.small.toString()
            userMediumProfilePic = profilePic?.medium.toString()
            userLargeProfilePic = profilePic?.large.toString()
            amenitiesRef = arrayListOf()
            answerAmenitiesRef = arrayListOf()

            binding.bookingLayout.visibility = View.GONE

        }else{

            reachAccount()

            binding.profileProgress.progress = 100
            bio = result?.data?.businessProfileRef?.bio ?: ""
            lat = result?.data?.businessProfileRef?.address?.lat ?: 0.0
            lng = result?.data?.businessProfileRef?.address?.lng ?: 0.0

            websiteLink = result?.data?.businessProfileRef?.website ?: ""
            if (bio.isNotEmpty()){
                val questionAdapter = QuestionLinesAdapter(this,bio)
                binding.questionRv.adapter = questionAdapter
            }

            binding.websiteBtn.visibility = if (websiteLink.isNotEmpty()) View.VISIBLE else View.GONE

//            dialCode = result.data?.businessProfileRef?.dialCode.toString()
//            phoneNumber = result.data?.businessProfileRef?.phoneNumber.toString()
            userFullName = result?.data?.businessProfileRef?.name.toString()
            val businessProfile = result?.data?.businessProfileRef
            val businessProfilePic = businessProfile?.profilePic
            amenitiesRef = businessProfile?.amenitiesRef ?: arrayListOf()
            answerAmenitiesRef = businessProfile?.businessAnswerRef ?: arrayListOf()
            rating = businessProfile?.rating ?: 0.0 // Replace with your dynamic rating
            binding.ratingTv.setRatingWithStarWithoutBracket(rating, R.drawable.ic_rating_star)


            userMediumProfilePic = businessProfilePic?.medium.toString()
            userLargeProfilePic = businessProfilePic?.large.toString()

            val tempMin = result?.data?.weather?.main?.feelsLike ?: 0.0
            val tempMax = result?.data?.weather?.main?.tempMax ?: 0.0
            val pm25Value = result?.data?.weather?.airPollution?.list?.getOrNull(0)?.components?.pm25 ?: 0.0
            val tempMinC = (tempMin - 273.15).roundToInt()
            val tempMaxC = (tempMax - 273.15).roundToInt()

            val overallAqi = pm25Value.toAQI()

            // Determine the span count and orientation based on the size of amenitiesRef
            val spanCount: Int
            val orientation: Int

            val staticAmenity = AmenitiesRef(
                Id = "static_id",
                icon = "static_icon_url",
                name = "Static Amenity",
                order = 0,
                minMaxTemp = "$tempMinC°C - $tempMaxC°C",
                aqi = overallAqi
            )
            amenitiesRef.add(0, staticAmenity)


            if (amenitiesRef.size > 6) {
                // If there are more than 6 items, use horizontal orientation
                orientation = GridLayoutManager.HORIZONTAL
                spanCount = 2  // You can adjust this as per your requirement for horizontal
            } else {
                // If there are 6 or fewer items, use vertical orientation
                orientation = GridLayoutManager.VERTICAL
                // Set the span count based on the size of amenitiesRef
                spanCount = when (amenitiesRef.size) {
                    1 -> 1
                    2, 3 -> 3
                    4 -> 4
                    5, 6 -> 3
                    else -> 1 // Default case, can be adjusted as needed
                }
            }

// Create and set the GridLayoutManager
            val layoutManager = GridLayoutManager(this, spanCount, orientation, false)
            binding.amenitiesRv.layoutManager = layoutManager

// Set the adapter
            val amenitiesAdapter = AmenitiesAdapter(this, amenitiesRef)
            binding.amenitiesRv.adapter = amenitiesAdapter

            if (myAccountType == business_type_individual){

//                if (BuildConfig.DEBUG){
//                    binding.bookingLayout.visibility = View.VISIBLE
//                }else{
//                    binding.bookingLayout.visibility = View.GONE
//                }

                binding.bookingLayout.visibility = View.VISIBLE
            }else{
                binding.bookingLayout.visibility = View.GONE
            }

        }
        val businessTypeID = result?.data?.businessProfileRef?.businessTypeID ?: ""
        val subBusinessTypeId = result?.data?.businessProfileRef?.businessSubTypeID ?: ""

//        binding.profileProgress.progress = profileCompleted?.toInt()!!
        binding.postCountTv.text = postCount.toString()
        binding.userNameTv.text = username
        binding.followerCountTv.text = followerCount.toString()
        binding.followingCountTv.text = followingCount.toString()


        val completed = getString(R.string.completed)
//        binding.completedTv.text = "( ${profileCompleted.toInt()}% $completed )"

        Glide.with(this).load(userMediumProfilePic).placeholder(R.drawable.ic_profile_placeholder).into(binding.profileIv)
        binding.fullNameTv.text = userFullName

        role = result?.data?.role ?: ""

        if (role == OFFICIAL){
            binding.menuBtn.visibility = View.GONE
            binding.followLayout.visibility = View.GONE
            binding.blankView.visibility = View.VISIBLE
            binding.verifyIcon.visibility = View.VISIBLE
        }else if (ownerUserId == outerUserId){
            binding.menuBtn.visibility = View.GONE
            binding.followLayout.visibility = View.GONE
            binding.blankView.visibility = View.VISIBLE
            binding.verifyIcon.visibility = View.GONE
        }else{
            binding.blankView.visibility = View.GONE
            binding.verifyIcon.visibility = View.GONE
        }

    }

    private fun notifyPostsFragmentFollowState() {
        val fragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        if (fragment is ProfilePostsFragment) {
            fragment.updateViewerFollowState(isConnected)
        }
    }

    private fun reachAccount() {
        if (businessProfileId.isNotEmpty()){
            individualViewModal.accountReach(businessProfileId)
        }
    }

    private fun getUserProfile() {
        // Use user profile endpoint first since the ID passed is typically a user ID
        // The user endpoint will return business profile data if it's a business account
        // The business endpoint requires the MongoDB ObjectId of the business profile, not the user ID
        if (outerUserId.isNotEmpty()) {
            individualViewModal.getUserProfileById(outerUserId)
        }
    }

    private fun applyFontFamily(tab: TabLayout.Tab, typeface: Typeface?, isSelected: Boolean) {
        val customView = tab.customView
        if (customView != null) {
            val tabTextView = customView.findViewById<TextView>(R.id.tab_text)
            val tabIconView = customView.findViewById<ImageView>(R.id.tab_icon)

            if (isSelected) {
                tabTextView.typeface = typeface
                tabTextView.setTextColor(ContextCompat.getColor(this, R.color.text_color))
                tabIconView.setColorFilter(ContextCompat.getColor(this, R.color.text_color))
            } else {
                tabTextView.typeface = typeface
                tabTextView.setTextColor(ContextCompat.getColor(this, R.color.text_hint_color))
                tabIconView.setColorFilter(ContextCompat.getColor(this, R.color.text_hint_color))
            }
        }
    }

    private fun setupTabBar() {

        val photos = getString(R.string.photos)
        val videos = getString(R.string.videos)
        val posts = getString(R.string.posts)
        val reviews = getString(R.string.reviews)
        val menu = getString(R.string.menu)

        val isRestaurant = businessName.equals(getString(R.string.restaurant), ignoreCase = true)

        if (accountType == business_type_individual) {
            binding.amenitiesTv.visibility = View.GONE
            binding.amenitiesRv.visibility = View.GONE
            binding.hotelTypeLayout.visibility = View.GONE
            binding.websiteBtn.visibility = View.GONE
            binding.writeReviewBtn.visibility = View.GONE
            tabTitles = arrayOf(photos, videos, posts,reviews)
            tabIcons = arrayOf(
                R.drawable.ic_photos,  // Replace with your actual icon resource
                R.drawable.ic_videos,  // Replace with your actual icon resource
                R.drawable.ic_posts,   // Replace with your actual icon resource
                R.drawable.ic_reviews  // Replace with your actual icon resource
            )
        } else {
            if (myAccountType == business_type_business ){
                binding.writeReviewBtn.visibility = View.GONE
            }else{
                binding.writeReviewBtn.visibility = View.VISIBLE
            }


            binding.amenitiesTv.visibility = View.VISIBLE
            binding.amenitiesRv.visibility = View.VISIBLE
            binding.hotelTypeLayout.visibility = View.VISIBLE
            binding.websiteBtn.visibility = View.VISIBLE

            // Add menu tab only for restaurants
            if (isRestaurant) {
                tabTitles = arrayOf(photos, videos, posts, reviews, menu)
                tabIcons = arrayOf(
                    R.drawable.ic_photos,
                    R.drawable.ic_videos,
                    R.drawable.ic_posts,
                    R.drawable.ic_reviews,
                    R.drawable.ic_photos  // Menu icon (using photos icon as placeholder)
                )
            } else {
                tabTitles = arrayOf(photos, videos, posts, reviews)
                tabIcons = arrayOf(
                    R.drawable.ic_photos,  // Replace with your actual icon resource
                    R.drawable.ic_videos,  // Replace with your actual icon resource
                    R.drawable.ic_posts,   // Replace with your actual icon resource
                    R.drawable.ic_reviews  // Replace with your actual icon resource
                )
            }
        }
        val comicRegular = ResourcesCompat.getFont(this, R.font.comic_regular)
        val comicMedium = ResourcesCompat.getFont(this, R.font.comic_regular)

        tab = binding.tabLayout

        // Remove previous tabs if any
        if (tab.tabCount > 0) {
            tab.removeAllTabs()
        }

        // Add tabs dynamically
        for (i in tabTitles.indices) {
            val customTab = LayoutInflater.from(this).inflate(R.layout.custom_tab_layout, null)
            val tabIcon = customTab.findViewById<ImageView>(R.id.tab_icon)
            val tabText = customTab.findViewById<TextView>(R.id.tab_text)

            tabIcon.setImageResource(tabIcons[i])
            tabText.text = tabTitles[i]

            tab.addTab(tab.newTab().setCustomView(customTab))
        }

        // Apply default colors for unselected tabs
        for (i in 0 until tab.tabCount) {
            applyFontFamily(tab.getTabAt(i)!!, comicRegular, false)
        }

        // Set tab text colors programmatically
        tab.setTabTextColors(
            ContextCompat.getColor(this, R.color.text_hint_color),
            ContextCompat.getColor(this, R.color.text_color)
        )

        // Select the first tab
        if (tab.tabCount > 0) {
            tab.getTabAt(0)?.select()
            replaceFragment(0)
            applyFontFamily(tab.getTabAt(0)!!, comicMedium, true)
        }

        tab.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                replaceFragment(tab.position)
                applyFontFamily(tab, comicMedium, true)
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {
                applyFontFamily(tab, comicRegular, false)
                tab.view.setBackgroundColor(Color.TRANSPARENT)
            }

            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }



    private fun replaceFragment(position: Int) {
        val isRestaurant = businessName.equals(getString(R.string.restaurant), ignoreCase = true)
        val totalTabs = if (accountType == business_type_individual) 4 else if (isRestaurant) 5 else 4
        
        val fragment = when {
            accountType == business_type_individual -> when (position) {
                0 -> ProfilePhotosFragment()
                1 -> ProfileVideosFragment()
                2 -> ProfilePostsFragment()
                3 -> ProfileReviewsFragment()
                else -> throw IllegalArgumentException("Invalid tab position")
            }
            isRestaurant -> when (position) {
                0 -> ProfilePhotosFragment()
                1 -> ProfileVideosFragment()
                2 -> ProfilePostsFragment()
                3 -> ProfileReviewsFragment()
                4 -> ProfileMenuFragment()
                else -> throw IllegalArgumentException("Invalid tab position")
            }
            else -> when (position) {
                0 -> ProfilePhotosFragment()
                1 -> ProfileVideosFragment()
                2 -> ProfilePostsFragment()
                3 -> ProfileReviewsFragment()
                else -> throw IllegalArgumentException("Invalid tab position")
            }
        }

        // Create a Bundle to pass the userId and businessProfileId
        val bundle = Bundle()
        bundle.putString("USER_ID", userId)
        bundle.putBoolean("IS_CONNECTED", isConnected)
        if (fragment is ProfileMenuFragment) {
            bundle.putString("BUSINESS_PROFILE_ID", businessProfileId)
        }
        fragment.arguments = bundle

        // Clear the back stack
        supportFragmentManager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    private fun getCheckInData() {
        if (placeID.isNotEmpty()){
            individualViewModal.getCheckInData(placeID,businessProfileId)
        }
    }

    private fun moveToFollowerFollowingActivity(from: String) {
        if (userId.isEmpty()){
            return
        }
        val intent = Intent(this, FollowerFollowingActivity::class.java)
        intent.putExtra("USERNAME",username)
        intent.putExtra("FROM",from)
        intent.putExtra("USERID",userId)
        startActivity(intent)
    }


    private fun showMenuDialog(view: View?) {
        // Inflate the dropdown menu layout
        val inflater = this.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val dropdownView = inflater.inflate(R.layout.post_menu_dropdown_item, null)

        // Create the PopupWindow
        val popupWindow = PopupWindow(
            dropdownView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )

        // Find TextViews and set click listeners
        val blockBtn: TextView = dropdownView.findViewById(R.id.blockBtn)
        val reportBtn: TextView = dropdownView.findViewById(R.id.reportBtn)
        val shareBtn: TextView = dropdownView.findViewById(R.id.shareBtn)
        val visitBtn: TextView = dropdownView.findViewById(R.id.visitBtn)

        if (isBlocked){
            blockBtn.text = getString(R.string.unblock)
        }else{
            blockBtn.text = getString(R.string.block)
        }

        if (accountType == business_type_individual){
            visitBtn.visibility = View.GONE
        }else{
            visitBtn.visibility = View.GONE
        }


        blockBtn.setOnClickListener {
            blockUser()
            popupWindow.dismiss()
        }

        reportBtn.setOnClickListener {
            reportUser()
            popupWindow.dismiss()
        }

        shareBtn.setOnClickListener {
            this.shareProfileWithDeepLink(outerUserId,ownerUserId)
            popupWindow.dismiss()
        }
        visitBtn.setOnClickListener {
            this.openGoogleMaps(lat, lng)
        }

        // Set the background drawable to make the popup more visually appealing
        popupWindow.setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.popup_background))

        // Show the popup window
        popupWindow.showAsDropDown(view)

        // Optionally, dismiss the popup when clicking outside of it
        popupWindow.setOnDismissListener {
            // Handle any actions you want to perform when the popup is dismissed
        }
    }

    private fun reportUser() {

        val bottomSheetFragment = ReportBottomSheetFragment().apply {
            arguments = Bundle().apply {
                putString("ID", outerUserId)
                putString("TYPE", "user")
            }
            onReasonSelected = { selectedReason ->
                individualViewModal.reportUser(outerUserId,selectedReason)
            }
        }
        bottomSheetFragment.show(supportFragmentManager, bottomSheetFragment.tag)

    }

    private fun blockUser() {
        val bottomSheetFragment = BlockUserBottomSheetFragment.newInstance(isBlocked,outerUserId)
        bottomSheetFragment.show(supportFragmentManager, bottomSheetFragment.tag)
    }

    override fun onBooleanDataReceived(isUserBlocked: Boolean) {
        // Handle the boolean returned from the fragment
        isBlocked = isUserBlocked
        getUserProfile()
    }


    private fun openAppOrBrowser(context: Context, packageName: String, url: String) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(url)
            setPackage(packageName)
        }
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            // If the app is not installed, open the URL in the browser
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(browserIntent)
        }
    }

}