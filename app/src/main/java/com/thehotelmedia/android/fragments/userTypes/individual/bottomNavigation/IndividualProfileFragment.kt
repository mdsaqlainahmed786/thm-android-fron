package com.thehotelmedia.android.fragments.userTypes.individual.bottomNavigation

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.google.android.material.tabs.TabLayout
import com.thehotelmedia.android.R
import com.thehotelmedia.android.ViewModelFactory
import com.thehotelmedia.android.activity.userTypes.individual.IndividualEditProfileActivity
import com.thehotelmedia.android.activity.userTypes.individual.IndividualSettingsActivity
import com.thehotelmedia.android.activity.userTypes.profile.FollowerFollowingActivity
import com.thehotelmedia.android.adapters.userTypes.individual.profile.AmenitiesAdapter
import com.thehotelmedia.android.adapters.userTypes.individual.profile.QuestionLinesAdapter
import com.thehotelmedia.android.customClasses.ColorFilterTransformation
import com.thehotelmedia.android.customClasses.Constants.business_type_individual
import com.thehotelmedia.android.customClasses.CustomProgressBar
import com.thehotelmedia.android.customClasses.PreferenceManager
import com.thehotelmedia.android.databinding.FragmentIndividualProfileBinding
import com.thehotelmedia.android.extensions.capitalizeFirstLetter
import com.thehotelmedia.android.extensions.setRatingWithStarWithoutBracket
import com.thehotelmedia.android.extensions.shareProfileWithDeepLink
import com.thehotelmedia.android.extensions.toAQI
import com.thehotelmedia.android.fragments.userTypes.individual.profile.ProfilePhotosFragment
import com.thehotelmedia.android.fragments.userTypes.individual.profile.ProfilePostsFragment
import com.thehotelmedia.android.fragments.userTypes.individual.profile.ProfileReviewsFragment
import com.thehotelmedia.android.fragments.userTypes.individual.profile.ProfileVideosFragment
import com.thehotelmedia.android.modals.profileData.profile.Address
import com.thehotelmedia.android.modals.profileData.profile.AmenitiesRef
import com.thehotelmedia.android.modals.profileData.profile.BusinessAnswerRef
import com.thehotelmedia.android.modals.profileData.profile.GetProfileModal
import com.thehotelmedia.android.repository.IndividualRepo
import com.thehotelmedia.android.viewModal.individualViewModal.IndividualViewModal
import kotlin.math.roundToInt

class IndividualProfileFragment : Fragment() {

    private lateinit var binding: FragmentIndividualProfileBinding
    private lateinit var tab: TabLayout
    private lateinit var preferenceManager : PreferenceManager
    private var accountType = ""
    private lateinit var tabTitles: Array<String>
    private lateinit var tabIcons: Array<Int>
    private lateinit var amenitiesRef: ArrayList<AmenitiesRef>
    private lateinit var answerAmenitiesRef: ArrayList<BusinessAnswerRef>
    private var userSmallProfilePic = ""
    private var userMediumProfilePic = ""
    private var userLargeProfilePic = ""
    private var dialCode = ""
    private var phoneNumber = ""
    private var userFullName = ""
    private var bio = ""
    private var userId = ""
    private var username = ""
    private var ownerUserId = ""
    private lateinit var individualViewModal: IndividualViewModal
    private lateinit var editProfileLauncher: ActivityResultLauncher<Intent>


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_individual_profile, container, false)


        val individualRepo = IndividualRepo(requireContext())
        individualViewModal = ViewModelProvider(requireActivity(), ViewModelFactory(null,individualRepo,null))[IndividualViewModal::class.java]
        preferenceManager = PreferenceManager.getInstance(requireContext())
        accountType = preferenceManager.getString(PreferenceManager.Keys.BUSINESS_TYPE, "").toString()
        binding.profileProgress.animate().rotationBy(-90f).setDuration(0).start()

        initUI()


        editProfileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // Retrieve the name returned from IndividualEditProfileActivity
                val name = result.data?.getStringExtra("updatedName")
                if(name == "EditProfile"){
                    initUI()
                }
            }
        }

        return binding.root
    }



    override fun onResume() {
        super.onResume()
//        initUI()
    }
    private fun initUI() {

        val progressBar = CustomProgressBar(requireActivity())

        userSmallProfilePic = preferenceManager.getString(PreferenceManager.Keys.USER_SMALL_PROFILE_PIC, "").toString()
        userMediumProfilePic = preferenceManager.getString(PreferenceManager.Keys.USER_MEDIUM_PROFILE_PIC, "").toString()
        userFullName = preferenceManager.getString(PreferenceManager.Keys.USER_FULL_NAME, "").toString()
        ownerUserId = preferenceManager.getString(PreferenceManager.Keys.USER_ID, "").toString()

        Glide.with(requireContext()).load(userMediumProfilePic).placeholder(R.drawable.ic_profile_placeholder).into(binding.profileIv)
        binding.fullNameTv.text = userFullName
        getProfileData()

        binding.settingsBtn.setOnClickListener {
            val intent = Intent(requireContext(), IndividualSettingsActivity::class.java)
            startActivity(intent)
        }
        binding.editProfileBtn.setOnClickListener {
//            val intent = Intent(requireContext(), MainActivity::class.java)
//            startActivity(intent)
            val intent = Intent(requireContext(), IndividualEditProfileActivity::class.java)
            editProfileLauncher.launch(intent)
        }

        binding.shareProfileBtn.setOnClickListener {
            requireContext().shareProfileWithDeepLink(ownerUserId,ownerUserId)
        }


        binding.followerBtnLayout.setOnClickListener {
            moveToFollowerFollowingActivity("Follower")
        }
        binding.followingBtnLayout.setOnClickListener {
            moveToFollowerFollowingActivity("Following")
        }
        binding.completedPercentageLayout.setOnClickListener {
            val intent = Intent(requireContext(), IndividualEditProfileActivity::class.java)
            editProfileLauncher.launch(intent)
        }



        individualViewModal.getProfileResult.observe(viewLifecycleOwner){result->
            if (result.status==true){
                handelProfileData(result)
            }else{
                val msg = result.message
                Toast.makeText(activity,msg, Toast.LENGTH_SHORT).show()
            }
        }


        individualViewModal.loading.observe(viewLifecycleOwner){
            if (it == true){
//                progressBar.show() // To show the progress bar
            }else{
//                progressBar.hide() // To hide the progress bar
            }
        }

        individualViewModal.toast.observe(viewLifecycleOwner){
            Toast.makeText(activity,it, Toast.LENGTH_SHORT).show()
        }


    }

    private fun moveToFollowerFollowingActivity(from: String) {
        val intent = Intent(requireContext(), FollowerFollowingActivity::class.java)
        intent.putExtra("USERNAME",username)
        intent.putExtra("FROM",from)
        intent.putExtra("USERID",userId)
        startActivity(intent)
    }


    private fun getProfileData() {
        individualViewModal.getProfile()
    }



//    private fun replaceFragment(position: Int) {
//        val fragment = when (position) {
//            0 -> ProfilePhotosFragment()
//            1 -> ProfileVideosFragment()
//            2 -> ProfilePostsFragment()
//            3 -> ProfileReviewsFragment()
//            else -> throw IllegalArgumentException("Invalid tab position")
//        }
//        childFragmentManager.beginTransaction()
//            .replace(R.id.fragment_container, fragment)
//            .commit()
//    }

    private fun replaceFragment(position: Int) {
        val fragment = when (position) {
            0 -> ProfilePhotosFragment()
            1 -> ProfileVideosFragment()
            2 -> ProfilePostsFragment()
            3 -> ProfileReviewsFragment()
            else -> throw IllegalArgumentException("Invalid tab position")
        }

        // Create a Bundle to pass the userId
        val bundle = Bundle()
        bundle.putString("USER_ID", userId)
        bundle.putString("FROM", "Profile")
        bundle.putBoolean("IS_CONNECTED", true)
        fragment.arguments = bundle
        // Clear the back stack
        childFragmentManager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)

        childFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    private fun applyFontFamily(tab: TabLayout.Tab, typeface: Typeface?, isSelected: Boolean) {
        val customView = tab.customView
        if (customView != null) {
            val tabTextView = customView.findViewById<TextView>(R.id.tab_text)
            val tabIconView = customView.findViewById<ImageView>(R.id.tab_icon)

            if (isSelected) {
                tabTextView.typeface = typeface
                tabTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_color))
                tabIconView.setColorFilter(ContextCompat.getColor(requireContext(), R.color.text_color))
            } else {
                tabTextView.typeface = typeface
                tabTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_hint_color))
                tabIconView.setColorFilter(ContextCompat.getColor(requireContext(), R.color.text_hint_color))
            }
        }
    }


    private fun handelProfileData(result: GetProfileModal) {
        userId = result.data?.Id ?: ""
        setupTabBar()
        val postCount = result.data?.posts
        val followerCount = result.data?.follower
        val followingCount = result.data?.following
        val privateAccount = result.data?.privateAccount
        val notificationEnabled = result.data?.notificationEnabled

        val profileCompleted = result.data?.profileCompleted
        accountType = result.data?.accountType?.capitalizeFirstLetter() ?: ""
        val profilePic = result.data?.profilePic

        username = result.data?.username.toString()
        val email = result.data?.email.toString()

        val rating = result.data?.businessProfileRef?.rating ?: 0.0

        binding.ratingTv.setRatingWithStarWithoutBracket(rating, R.drawable.ic_rating_star)

        val businessTypeRef = result.data?.businessProfileRef?.businessTypeRef
        val businessName = businessTypeRef?.name
        val businessIcon = businessTypeRef?.icon


        binding.businessNameTv.text = businessName
        Glide.with(requireContext()).load(businessIcon)
            .placeholder(R.drawable.ic_hotel)
            .transform(ColorFilterTransformation(ContextCompat.getColor(requireContext(), R.color.text_color)))
            .into(binding.businessIconIv)


        bio = result.data?.bio.toString()
        if (bio.isNotEmpty()){
            val questionAdapter = QuestionLinesAdapter(requireContext(),bio)
            binding.questionRv.adapter = questionAdapter
        }



        if (accountType == business_type_individual){
            bio = result.data?.bio.toString()
            if (bio.isNotEmpty()){
                val questionAdapter = QuestionLinesAdapter(requireContext(),bio)
                binding.questionRv.adapter = questionAdapter
            }
            userFullName = result.data?.name.toString()
            dialCode = result.data?.dialCode.toString()
            phoneNumber = result.data?.phoneNumber.toString()
            userSmallProfilePic = profilePic?.small.toString()
            userMediumProfilePic = profilePic?.medium.toString()
            userLargeProfilePic = profilePic?.large.toString()
            amenitiesRef = arrayListOf()
            answerAmenitiesRef = arrayListOf()


            val userAddress = result.data?.address



            saveIndividualAddress(userAddress)
        }else{
            bio = result.data?.businessProfileRef?.bio.toString()
            if (bio.isNotEmpty()){
                val questionAdapter = QuestionLinesAdapter(requireContext(),bio)
                binding.questionRv.adapter = questionAdapter
            }
            dialCode = result.data?.businessProfileRef?.dialCode.toString()
            phoneNumber = result.data?.businessProfileRef?.phoneNumber.toString()
            userFullName = result.data?.businessProfileRef?.name.toString()
            val businessProfile = result.data?.businessProfileRef
            val businessProfilePic = businessProfile?.profilePic
            amenitiesRef = businessProfile?.amenitiesRef!!
            answerAmenitiesRef = businessProfile.businessAnswerRef




            userSmallProfilePic = businessProfilePic?.small.toString()
            userMediumProfilePic = businessProfilePic?.medium.toString()
            userLargeProfilePic = businessProfilePic?.large.toString()


//// Determine the span count based on the size of amenitiesRef
//            val spanCount = if ((amenitiesRef.size ?: 0) > 4) 2 else 1
//
//// Create and set the GridLayoutManager with horizontal orientation
//            val layoutManager = GridLayoutManager(requireContext(), spanCount, GridLayoutManager.HORIZONTAL, false)
//            binding.amenitiesRv.layoutManager = layoutManager
//
//            val amenitiesAdapter = AmenitiesAdapter(requireContext(), amenitiesRef)
//            binding.amenitiesRv.adapter = amenitiesAdapter

            val tempMin = result.data?.weather?.main?.feelsLike ?: 0.0
            val tempMax = result.data?.weather?.main?.tempMax ?: 0.0
            val pm25Value = result.data?.weather?.airPollution?.list?.get(0)?.components?.pm25 ?: 0.0
            val tempMinC = (tempMin - 273.15).roundToInt()
            val tempMaxC = (tempMax - 273.15).roundToInt()
            val overallAqi = pm25Value.toAQI()

            // Determine the span count and orientation based on the size of amenitiesRef
            val spanCount: Int
            val orientation: Int



            val alreadyExists = amenitiesRef.any { it.Id == "static_id" }

            if (!alreadyExists) {
                val staticAmenity = AmenitiesRef(
                    Id = "static_id",
                    icon = "static_icon_url",
                    name = "Static Amenity",
                    order = 0,
                    minMaxTemp = "$tempMinC°C - $tempMaxC°C",
                    aqi = overallAqi
                )
                amenitiesRef.add(0, staticAmenity)
            }

            if ((amenitiesRef.size ?: 0) > 6) {
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
            val layoutManager = GridLayoutManager(requireContext(), spanCount, orientation, false)
            binding.amenitiesRv.layoutManager = layoutManager

// Set the adapter
            val amenitiesAdapter = AmenitiesAdapter(requireContext(), amenitiesRef)
            binding.amenitiesRv.adapter = amenitiesAdapter




        }
        val businessTypeID = result.data?.businessProfileRef?.businessTypeID ?: ""
        val subBusinessTypeId = result.data?.businessProfileRef?.businessSubTypeID ?: ""



        binding.profileProgress.progress = profileCompleted?.toInt()!!
        binding.postCountTv.text = postCount.toString()
        binding.userNameTv.text = username
        binding.followerCountTv.text = followerCount.toString()
        binding.followingCountTv.text = followingCount.toString()


        val completed = getString(R.string.completed)
        binding.completedTv.text = "( ${profileCompleted.toInt()}% $completed )"
        if (profileCompleted.toInt() == 100){
            binding.completedPercentageLayout.visibility = View.GONE
            binding.profileProgress.visibility = View.INVISIBLE
        }else{
            binding.completedPercentageLayout.visibility = View.VISIBLE
            binding.profileProgress.visibility = View.VISIBLE
        }

        Glide.with(requireContext()).load(userMediumProfilePic).placeholder(R.drawable.ic_profile_placeholder).into(binding.profileIv)
        binding.fullNameTv.text = userFullName

        saveDataInSharedPreference(email,dialCode,phoneNumber,bio,username,businessName,businessTypeID,subBusinessTypeId,privateAccount,notificationEnabled)

    }

    private fun saveIndividualAddress(userAddress: Address?) {

        val userStreet = userAddress?.street ?: ""
        val userCity = userAddress?.city ?: ""
        val userState = userAddress?.state ?: ""
        val userZipCode = userAddress?.zipCode ?: ""
        val userCountry = userAddress?.country ?: ""
        val userLat = userAddress?.lat ?: 0.0
        val userLng = userAddress?.lng ?: 0.0

        preferenceManager.putString(PreferenceManager.Keys.USER_STREET, userStreet)
        preferenceManager.putString(PreferenceManager.Keys.USER_CITY, userCity)
        preferenceManager.putString(PreferenceManager.Keys.USER_STATE, userState)
        preferenceManager.putString(PreferenceManager.Keys.USER_COUNTRY, userCountry)
        preferenceManager.putString(PreferenceManager.Keys.USER_ZIPCODE, userZipCode)
        preferenceManager.putString(PreferenceManager.Keys.USER_LATITUDE, userLat.toString())
        preferenceManager.putString(PreferenceManager.Keys.USER_LONGITUDE, userLng.toString())

    }

    private fun saveDataInSharedPreference(
        email: String,
        dialCode: String,
        phoneNumber: String,
        bio: String,
        username: String,
        businessName: String?,
        businessTypeID: String?,
        subBusinessTypeId: String?,
        privateAccount: Boolean?,
        notificationEnabled: Boolean?
    ) {



        if (amenitiesRef.isNotEmpty()){
            preferenceManager.putAmenitiesList(PreferenceManager.Keys.AMENITIES_REF_LIST, amenitiesRef)
            preferenceManager.putAnswerAmenitiesList(PreferenceManager.Keys.ANSWER_AMENITIES_REF_LIST, answerAmenitiesRef)
        }


        preferenceManager.putString(PreferenceManager.Keys.USER_BUSINESS_NAME, businessName.toString())
        preferenceManager.putString(PreferenceManager.Keys.USER_USER_NAME, username)
        preferenceManager.putString(PreferenceManager.Keys.USER_FULL_NAME, userFullName)
        preferenceManager.putString(PreferenceManager.Keys.USER_EMAIL, email)
        preferenceManager.putString(PreferenceManager.Keys.USER_DIAL_CODE, dialCode)
        preferenceManager.putString(PreferenceManager.Keys.USER_PHONE_NUMBER, phoneNumber)
        preferenceManager.putString(PreferenceManager.Keys.USER_DESCRIPTION, bio)

        preferenceManager.putString(PreferenceManager.Keys.USER_SMALL_PROFILE_PIC, userSmallProfilePic)
        preferenceManager.putString(PreferenceManager.Keys.USER_MEDIUM_PROFILE_PIC, userMediumProfilePic)
        preferenceManager.putString(PreferenceManager.Keys.USER_LARGE_PROFILE_PIC, userLargeProfilePic)

        preferenceManager.putBoolean(PreferenceManager.Keys.IS_PRIVATE_ACCOUNT, privateAccount ?: false)
        preferenceManager.putBoolean(PreferenceManager.Keys.IS_NOTIFICATION_ENABLED, notificationEnabled ?: false)


    }



//    private fun setupTabBar() {
//
//        val photos = getString(R.string.photos)
//        val videos = getString(R.string.videos)
//        val posts = getString(R.string.posts)
//        val reviews = getString(R.string.reviews)
//
//        if (accountType == business_type_individual) {
//            binding.amenitiesTv.visibility = View.GONE
//            binding.amenitiesRv.visibility = View.GONE
//            binding.hotelTypeLayout.visibility = View.GONE
//            tabTitles = arrayOf(photos, videos, posts,reviews)
//            tabIcons = arrayOf(
//                R.drawable.ic_photos,  // Replace with your actual icon resource
//                R.drawable.ic_videos,  // Replace with your actual icon resource
//                R.drawable.ic_posts,   // Replace with your actual icon resource
//                R.drawable.ic_reviews    // Replace with your actual icon resource
//            )
//        } else {
//            binding.amenitiesTv.visibility = View.VISIBLE
//            binding.amenitiesRv.visibility = View.VISIBLE
//            binding.hotelTypeLayout.visibility = View.VISIBLE
//            tabTitles = arrayOf(photos, videos, posts, reviews)
//            tabIcons = arrayOf(
//                R.drawable.ic_photos,  // Replace with your actual icon resource
//                R.drawable.ic_videos,  // Replace with your actual icon resource
//                R.drawable.ic_posts,   // Replace with your actual icon resource
//                R.drawable.ic_reviews  // Replace with your actual icon resource
//            )
//        }
//
//        val comicRegular = ResourcesCompat.getFont(requireContext(), R.font.comic_regular)
//        val comicMedium = ResourcesCompat.getFont(requireContext(), R.font.comic_regular)
//
//        tab = binding.tabLayout
//
//
//        // Check if tabs are already present and remove only if they exist
//        if (tab.tabCount > 0) {
//            tab.removeAllTabs() // Clear any previously added tabs
//        }
//
//        // Dynamically add tabs with custom view (icon + text)
//        for (i in tabTitles.indices) {
//            val customTab = LayoutInflater.from(requireContext()).inflate(R.layout.custom_tab_layout, null)
//            val tabIcon = customTab.findViewById<ImageView>(R.id.tab_icon)
//            val tabText = customTab.findViewById<TextView>(R.id.tab_text)
//
//            tabIcon.setImageResource(tabIcons[i])
//            tabText.text = tabTitles[i]
//
//            tab.addTab(this.tab.newTab().setCustomView(customTab))
//        }
//
//        // Load the first tab by default if any tabs exist
//        if (tab.tabCount > 0) {
//            tab.getTabAt(0)?.select()  // Select the first tab by default
//            replaceFragment(0) // Load the first fragment ("Photos") by default
//            applyFontFamily(tab.getTabAt(0)!!, comicMedium, true) // Apply selected font style and color
//        }
//
//        tab.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
//            override fun onTabSelected(tab: TabLayout.Tab) {
//                replaceFragment(tab.position)
//                applyFontFamily(tab, comicMedium, true)
//            }
//
//            override fun onTabUnselected(tab: TabLayout.Tab) {
//                applyFontFamily(tab, comicRegular, false)
//                tab.view.setBackgroundColor(Color.TRANSPARENT)
//            }
//
//            override fun onTabReselected(tab: TabLayout.Tab) {}
//        })
//    }

    private fun setupTabBar() {



        if (accountType == business_type_individual) {
            binding.amenitiesTv.visibility = View.GONE
            binding.amenitiesRv.visibility = View.GONE
            binding.hotelTypeLayout.visibility = View.GONE
        } else {
            binding.amenitiesTv.visibility = View.VISIBLE
            binding.amenitiesRv.visibility = View.VISIBLE
            binding.hotelTypeLayout.visibility = View.VISIBLE
        }



        val photos = getString(R.string.photos)
        val videos = getString(R.string.videos)
        val posts = getString(R.string.posts)
        val reviews = getString(R.string.reviews)

        tabTitles = arrayOf(photos, videos, posts, reviews)
        tabIcons = arrayOf(
            R.drawable.ic_photos,
            R.drawable.ic_videos,
            R.drawable.ic_posts,
            R.drawable.ic_reviews
        )

        val comicRegular = ResourcesCompat.getFont(requireContext(), R.font.comic_regular)
        val comicMedium = ResourcesCompat.getFont(requireContext(), R.font.comic_regular)

        tab = binding.tabLayout

        // Remove previous tabs if any
        if (tab.tabCount > 0) {
            tab.removeAllTabs()
        }

        // Add tabs dynamically
        for (i in tabTitles.indices) {
            val customTab = LayoutInflater.from(requireContext()).inflate(R.layout.custom_tab_layout, null)
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
            ContextCompat.getColor(requireContext(), R.color.text_hint_color),
            ContextCompat.getColor(requireContext(), R.color.text_color)
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

}

