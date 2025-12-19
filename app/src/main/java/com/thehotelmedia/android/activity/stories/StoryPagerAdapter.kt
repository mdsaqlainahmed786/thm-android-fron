package com.thehotelmedia.android.activity.stories

import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import android.text.Spannable
import android.text.SpannableString
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.StyledPlayerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout.LayoutParams
import android.widget.ProgressBar
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.thehotelmedia.android.R
import com.thehotelmedia.android.Socket.SocketViewModel
import com.thehotelmedia.android.activity.BusinessProfileDetailsActivity
import com.thehotelmedia.android.activity.userTypes.business.bottomNavigation.BottomNavigationBusinessMainActivity
import com.thehotelmedia.android.activity.userTypes.individual.bottomNavigation.BottomNavigationIndividualMainActivity
import com.thehotelmedia.android.bottomSheets.StoriesLikeReactionFragment
import com.thehotelmedia.android.bottomSheets.TwoVerticalOptionBottomSheetFragment
import com.thehotelmedia.android.bottomSheets.YesOrNoBottomSheetFragment
import com.thehotelmedia.android.customClasses.Constants.business_type_individual
import com.thehotelmedia.android.customClasses.MessageStore
import com.thehotelmedia.android.customClasses.PreferenceManager
import com.thehotelmedia.android.databinding.StoryScreenLayoutBinding
import com.thehotelmedia.android.extensions.capitalizeFirstLetter
import com.thehotelmedia.android.extensions.getTimeAgo
import com.thehotelmedia.android.modals.Stories.Stories
import com.thehotelmedia.android.modals.Stories.StoriesRef
import com.thehotelmedia.android.modals.feeds.feed.TaggedRef
import com.thehotelmedia.android.viewModal.individualViewModal.IndividualViewModal
import com.google.gson.Gson
import com.thehotelmedia.android.bottomSheets.TagPeopleBottomSheetFragment

class StoryPagerAdapter(
    private val context: ViewStoriesActivity,
    private val userList: List<Stories>, // List of media items (image/video URL and duration)
    private val viewPager: ViewPager2,
    private val preferenceManager: PreferenceManager,
    private val individualViewModal: IndividualViewModal,
    private val supportFragmentManager: FragmentManager,
    private val onItemClicked: (Boolean) -> Unit,
    private val socketViewModel: SocketViewModel
) : RecyclerView.Adapter<StoryPagerAdapter.ViewHolder>() {

    private var progressBars = mutableListOf<ProgressBar>()
    private var currentStoryIndex = 0
    private var currentAnimator: ValueAnimator? = null
    private var isVideoPlaying = false
    private var isMovedToMainScreen = false
    // Track the last known page index
    private var previousPageIndex = -1
    private var userAccountType = ""
    private var verticalTwoOptionBottomSheet: TwoVerticalOptionBottomSheetFragment? = null
    private var currentExoPlayer: ExoPlayer? = null

    inner class ViewHolder(val binding: StoryScreenLayoutBinding) : RecyclerView.ViewHolder(binding.root)

    init {
        // Listener for ViewPager2 page changes to stop animations when switching users
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)

                // Stop previous animations when moving to a new user
                if (previousPageIndex != position) {
                    stopCurrentStory()

                    previousPageIndex = position
                    // Reset story index and progress bar when switching users
                    currentStoryIndex = 0
                    notifyItemChanged(position)
                }
            }
        })
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = StoryScreenLayoutBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val binding = holder.binding
        val users = userList[position]
        val storyMedia = userList[position].storiesRef

        var userName = ""
        var socketUserName = ""
        var profilePic = ""
        var likeCount = ""
        var viewerCount = ""
        var storyId =  ""
        var mediaId =  ""
        var sourceUrl =  ""
        var createdAt= ""

        if (currentStoryIndex in users.storiesRef.indices) {
            val story = users.storiesRef[currentStoryIndex]
            storyId = story.Id ?: ""
            mediaId = story.mediaID ?: ""
            sourceUrl = story.sourceUrl ?: ""
            createdAt = story.createdAt ?: ""
        } else {
            Log.e("StoryAdapter", "Invalid index: $currentStoryIndex, list size: ${users.storiesRef.size}")
        }

        val userId =  users.id ?: ""
        userAccountType = preferenceManager.getString(PreferenceManager.Keys.BUSINESS_TYPE,"").toString()

        var formatedCreatedAt = ""
        if (createdAt.isNotEmpty()){
            formatedCreatedAt = getTimeAgo(createdAt)
        }

        val accountType = users.accountType?.capitalizeFirstLetter() ?: ""

        viewStory(storyId)

        if (accountType == "Owner"){
            val imageViews = listOf(
                binding.imageView1,
                binding.imageView2,
                binding.imageView3
            )

//            val likesRef = users.storiesRef[currentStoryIndex].likesRef
            val viewsRef = users.storiesRef[currentStoryIndex].viewsRef

            userName = preferenceManager.getString(PreferenceManager.Keys.USER_FULL_NAME,"").toString()
            profilePic = preferenceManager.getString(PreferenceManager.Keys.USER_LARGE_PROFILE_PIC,"").toString()

            binding.nameTv.visibility = View.GONE
            binding.menuBtn.visibility = View.GONE
            binding.otherUserCommentLayout.visibility = View.GONE
            binding.deleteBtn.visibility = View.VISIBLE
            likeCount =  users.storiesRef[currentStoryIndex].likes.toString()
            viewerCount =  users.storiesRef[currentStoryIndex].views.toString()

            if (likeCount == "0" && viewerCount == "0"){
                binding.viewerLayout.visibility = View.GONE
            }else{
                binding.viewerLayout.visibility = View.VISIBLE
            }

            if (viewsRef.isEmpty()){
                binding.likedLayout.visibility = View.GONE
            }else{
                binding.likedLayout.visibility = View.VISIBLE
                // Get the minimum value between the size of eventJoiningRef and 6 (to show only the first 6 people)
                val numberOfPeopleToShow = viewsRef.size.coerceAtMost(3)
                // Loop through the imageViews and assign profile pictures
                for (i in 0 until 3) {
                    if (i < numberOfPeopleToShow) {
                        // Set the profile picture from the list
                        val person = viewsRef[i]
                        val profilePicUrl = person.profilePic?.medium // Assuming 'url' is the field in ProfilePic

                        // Load the profile picture into the ImageView (using Glide or another image loading library)
                        Glide.with(context)
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
            }

        }else{

            if (accountType == business_type_individual){
                userName = users.name ?: ""
                socketUserName = users.username ?: ""
                profilePic= users.profilePic?.large ?: ""
            }else{
                userName = users.businessProfileRef?.name ?: ""
                socketUserName = users.businessProfileRef?.username ?: ""
                profilePic= users.businessProfileRef?.profilePic?.large ?: ""
            }

            binding.nameTv.visibility = View.VISIBLE
            binding.menuBtn.visibility = View.VISIBLE
            binding.otherUserCommentLayout.visibility = View.VISIBLE
            binding.deleteBtn.visibility = View.GONE
            binding.viewerLayout.visibility = View.GONE
        }

        binding.deleteBtn.setOnClickListener {
            showBottomSheet(storyId,binding)
        }

        binding.menuBtn.setOnClickListener {
            showBlockandViewProfileBottomSheet(userId,binding,userName)
        }

        binding.viewerLayout.setOnClickListener {
            showViewerBottomSheet(storyId,binding,likeCount,viewerCount)
        }

        // Make the comment button (BlurView) clickable to open the comment input
        binding.commentBtn?.setOnClickListener {
            binding.commentEt?.requestFocus()
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(binding.commentEt, InputMethodManager.SHOW_IMPLICIT)
        }

        binding.commentEt.setOnFocusChangeListener { _, hasFocus ->
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            if (hasFocus) {
                pauseStory(binding)
                currentAnimator?.pause()
                // Show the keyboard when the EditText gains focus
                imm.showSoftInput(binding.commentEt, InputMethodManager.SHOW_IMPLICIT)
            } else {
                // Hide the keyboard when the EditText loses focus
                imm.hideSoftInputFromWindow(binding.commentEt.windowToken, 0)
                resumeStory(binding)
            }
        }

        binding.commentEt.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                val commentText = binding.commentEt.text.toString().trim()
                if (commentText.isNotEmpty()) {
                    // Send comment via socket
                    socketViewModel.sendStoryComment("story-comment",commentText,socketUserName,sourceUrl,mediaId,storyId)
                    
                    // Clear the EditText after sending
                    binding.commentEt.text?.clear()
                    resumeStory(binding)

                    // Hide the keyboard after sending
                    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(binding.commentEt.windowToken, 0)
                }

                true // Return true to indicate the action was handled
            } else {
                false
            }
        }


        binding.nameTv.text = userName
        binding.timingTv.text = formatedCreatedAt
        Glide.with(context).load(profilePic).placeholder(R.drawable.ic_profile_placeholder).into(binding.profilePic)

        // Display tagged people if they exist
        if (currentStoryIndex in users.storiesRef.indices) {
            val story = users.storiesRef[currentStoryIndex]
            android.util.Log.d("StoryPagerAdapter", "=== BINDING STORY ===")
            android.util.Log.d("StoryPagerAdapter", "Story ID: ${story.Id}, currentStoryIndex: $currentStoryIndex")
            android.util.Log.d("StoryPagerAdapter", "Full story object: $story")
            
            val taggedPeople = story.taggedRef ?: arrayListOf()
            if (taggedPeople.isNotEmpty()) {
                setupTaggedPeopleLinks(binding, taggedPeople)
            } else {
                binding.taggedPeopleTv.visibility = View.GONE
                binding.taggedPeopleTv.text = ""
                binding.taggedPeopleTv.setOnClickListener(null)
            }
            
            // Always hide location views first, then show if location exists
            hideLocationViews(binding)
            
            // Display location if it exists - use floating button
            val location = story.location
            android.util.Log.d("StoryPagerAdapter", "=== LOCATION PARSING DEBUG ===")
            android.util.Log.d("StoryPagerAdapter", "Story ID: ${story.Id}")
            android.util.Log.d("StoryPagerAdapter", "Story location object: $location")
            android.util.Log.d("StoryPagerAdapter", "Location is null: ${location == null}")
            
            if (location != null) {
                android.util.Log.d("StoryPagerAdapter", "Location placeName: ${location.placeName}")
                android.util.Log.d("StoryPagerAdapter", "Location lat: ${location.lat} (type: ${location.lat?.javaClass?.simpleName})")
                android.util.Log.d("StoryPagerAdapter", "Location lng: ${location.lng} (type: ${location.lng?.javaClass?.simpleName})")
            }
            
            // CRITICAL: Only show location button if we have valid location data
            if (location != null && location.placeName != null && location.placeName!!.isNotEmpty()) {
                android.util.Log.d("StoryPagerAdapter", "✓ Location found - placeName: '${location.placeName}', lat: ${location.lat}, lng: ${location.lng}")
                
                // StoryLocation uses Double? directly, no conversion needed
                val lat = location.lat
                val lng = location.lng
                
                android.util.Log.d("StoryPagerAdapter", "Coordinates - lat: $lat, lng: $lng")
                
                if (lat != null && lng != null && lat.isFinite() && lng.isFinite()) {
                    android.util.Log.d("StoryPagerAdapter", "✓✓✓ VALID coordinates - Setting up location button with:")
                    android.util.Log.d("StoryPagerAdapter", "   placeName: '${location.placeName}'")
                    android.util.Log.d("StoryPagerAdapter", "   lat: $lat")
                    android.util.Log.d("StoryPagerAdapter", "   lng: $lng")
                    // Only use the floating location button (locationButtonCard), not the TextView (locationTv)
                    setupLocationFloatingButton(binding, location.placeName!!, lat, lng)
                    android.util.Log.d("StoryPagerAdapter", "✓✓✓ Location button setup complete!")
                } else {
                    android.util.Log.e("StoryPagerAdapter", "✗ Invalid lat/lng values: lat=$lat, lng=$lng - HIDING location button")
                    hideLocationViews(binding) // Ensure button is hidden if coordinates are invalid
                }
            } else {
                android.util.Log.d("StoryPagerAdapter", "✗ No valid location data - location: $location, placeName: ${location?.placeName} - HIDING location button")
                
                // WORKAROUND: Check if location button card is visible (from image overlay)
                // If it is, we need to make it clickable even if API doesn't return location
                val locationButtonCard = binding.root.findViewById<com.google.android.material.card.MaterialCardView>(R.id.locationButtonCard)
                val locationButtonText = binding.root.findViewById<android.widget.TextView>(R.id.locationButtonText)
                
                if (locationButtonCard != null && locationButtonCard.visibility == View.VISIBLE) {
                    android.util.Log.w("StoryPagerAdapter", "⚠️ Location button card is VISIBLE but location data is NULL!")
                    android.util.Log.w("StoryPagerAdapter", "⚠️ This means location tag is from image overlay, but API didn't return location data")
                    android.util.Log.w("StoryPagerAdapter", "⚠️ Cannot make it clickable without location coordinates")
                    android.util.Log.w("StoryPagerAdapter", "⚠️ Story ID: ${story.Id}")
                    android.util.Log.w("StoryPagerAdapter", "⚠️ Please check backend - location should be returned in API response")
                }
                
                hideLocationViews(binding) // Explicitly hide if no location data
            }
            android.util.Log.d("StoryPagerAdapter", "=== END LOCATION PARSING DEBUG ===")
        } else {
            binding.taggedPeopleTv.visibility = View.GONE
            binding.taggedPeopleTv.text = ""
            binding.taggedPeopleTv.setOnClickListener(null)
            binding.locationTv.visibility = View.GONE
            binding.locationTv.text = ""
            binding.locationTv.setOnClickListener(null)
        }

        // Reset progress bars and stop any ongoing animations/media
        resetProgressBars(storyMedia.size, binding)

        // Start the first story for the current user
        showCurrentStory(storyMedia, binding,users)

        setClickListener(binding,users)

        binding.backBtn.setOnClickListener {
            moveToMainScreen()
        }

    }

    private fun showBlockandViewProfileBottomSheet(
        userId: String,
        binding: StoryScreenLayoutBinding,
        userName: String
    ) {
        pauseStory(binding)
        verticalTwoOptionBottomSheet = TwoVerticalOptionBottomSheetFragment.newInstance(
            title = "",
            blockButtonText = MessageStore.blockUser(context),
            viewProfileButtonText = MessageStore.viewProfile(context)
        ).apply {
            onBlockClick = {
                // Handle block click

                verticalTwoOptionBottomSheet?.dismiss()

                pauseStory(binding)

                // Show the BottomSheetFragment
                val bottomSheet = YesOrNoBottomSheetFragment.newInstance("${this@StoryPagerAdapter.context.getString(R.string.do_you_want_to_block)} $userName?")
                bottomSheet.onYesClicked = {
                    // Handle Yes button click
                    individualViewModal.blockUser(userId.toString())
                }
                bottomSheet.onNoClicked = {
                    resumeStory(binding)
                }
                bottomSheet.show(supportFragmentManager, "YesOrNoBottomSheet")
            }
            onViewProfileClick = {
                moveToProfileDetailsActivity(userId)
            }
            onDismissCallback = {
                resumeStory(binding)
            }
        }
        verticalTwoOptionBottomSheet?.show(supportFragmentManager, "TwoVerticalOptionBottomSheet")
    }

    private fun moveToProfileDetailsActivity(userId: String) {
        val intent = Intent(context, BusinessProfileDetailsActivity::class.java)
        intent.putExtra("USER_ID", userId)
        context.startActivity(intent)
    }

    private fun showViewerBottomSheet(storyId: String, binding: StoryScreenLayoutBinding, likeCount: String, viewsCount: String) {
        pauseStory(binding)
        val storiesLikeReactionFragment = StoriesLikeReactionFragment.newInstance(storyId,likeCount,viewsCount)
        storiesLikeReactionFragment.onDismissCallback = {
            resumeStory(binding)
        }
        storiesLikeReactionFragment.show(supportFragmentManager, StoriesLikeReactionFragment::class.java.simpleName)
    }

    fun moveToMainScreen() {
        if (!isMovedToMainScreen){
            isMovedToMainScreen = true
            // Release all players before navigating away
            releaseAllPlayers()
            if (userAccountType == business_type_individual){
                val intent = Intent(context, BottomNavigationIndividualMainActivity::class.java)
                context.startActivity(intent)
                context.finish()
            }else{
                val intent = Intent(context, BottomNavigationBusinessMainActivity::class.java)
                context.startActivity(intent)
                context.finish()
            }
        }
    }
    
    fun releaseAllPlayers() {
        // Stop and release current ExoPlayer
        currentExoPlayer?.stop()
        currentExoPlayer?.release()
        currentExoPlayer = null
        isVideoPlaying = false
        
        // Cancel any running animations
        currentAnimator?.cancel()
        currentAnimator = null
    }

    private data class TagSpanInfo(
        val start: Int,
        val end: Int,
        val userId: String
    )

    /**
     * Build "With Name1, Name2" text where each name is a blue, clickable link
     * that opens the tagged user's profile.
     */
    private fun setupLocationView(binding: StoryScreenLayoutBinding, placeName: String, lat: Double, lng: Double) {
        binding.locationTv.text = placeName
        binding.locationTv.visibility = View.VISIBLE
        binding.locationTv.isClickable = true
        binding.locationTv.isFocusable = true
        binding.locationTv.isFocusableInTouchMode = true
        
        // Bring the topLayout to front so interactive elements can receive touches
        binding.topLayout.bringToFront()
        binding.topLayout.isClickable = false
        binding.topLayout.isFocusable = false
        
        // Handle touch events to consume them and prevent parent views from handling
        binding.locationTv.setOnTouchListener { view, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    // Consume the DOWN event to prevent parent views from handling it
                    view.parent?.requestDisallowInterceptTouchEvent(true)
                    true
                }
                android.view.MotionEvent.ACTION_UP -> {
                    view.parent?.requestDisallowInterceptTouchEvent(false)
                    // Pause story while opening maps
                    pauseStory(binding)
                    
                    // Open location in Google Maps
                    openLocationInMaps(placeName, lat, lng)
                    true // Consume the event
                }
                android.view.MotionEvent.ACTION_CANCEL -> {
                    view.parent?.requestDisallowInterceptTouchEvent(false)
                    false
                }
                else -> false
            }
        }
        
        // Also set click listener as backup
        binding.locationTv.setOnClickListener {
            // Pause story while opening maps
            pauseStory(binding)
            // Open location in Google Maps
            openLocationInMaps(placeName, lat, lng)
        }
    }
    
    private fun hideLocationViews(binding: StoryScreenLayoutBinding) {
        android.util.Log.d("StoryPagerAdapter", "Hiding location views")
        // Always hide the TextView location (locationTv) - we only use the floating button
        binding.locationTv.visibility = View.GONE
        binding.locationTv.text = ""
        binding.locationTv.setOnClickListener(null)
        binding.locationTv.setOnTouchListener(null)
        
        // Hide the floating location button (locationButtonCard)
        val locationButtonCard = binding.root.findViewById<com.google.android.material.card.MaterialCardView>(R.id.locationButtonCard)
        if (locationButtonCard != null) {
            locationButtonCard.visibility = View.GONE
            locationButtonCard.setOnClickListener(null)
            locationButtonCard.setOnTouchListener(null)
            locationButtonCard.setOnLongClickListener(null)
            // Clear the text
            val locationButtonText = binding.root.findViewById<android.widget.TextView>(R.id.locationButtonText)
            locationButtonText?.text = ""
            android.util.Log.d("StoryPagerAdapter", "Location button hidden and listeners cleared")
        }
    }
    
    private fun setupLocationFloatingButton(binding: StoryScreenLayoutBinding, placeName: String, lat: Double, lng: Double) {
        // Use findViewById to get the views since data binding might not have generated them yet
        val locationButtonCard = binding.root.findViewById<com.google.android.material.card.MaterialCardView>(R.id.locationButtonCard)
        val locationButtonText = binding.root.findViewById<android.widget.TextView>(R.id.locationButtonText)
        
        android.util.Log.d("StoryPagerAdapter", "Setting up location button - card found: ${locationButtonCard != null}, text found: ${locationButtonText != null}")
        
        if (locationButtonCard != null && locationButtonText != null) {
            locationButtonCard.visibility = View.VISIBLE
            locationButtonText.text = placeName
            
            android.util.Log.d("StoryPagerAdapter", "Location button visible and text set to: $placeName")
            
            // Ensure the button is visible and on top
            locationButtonCard.post {
                locationButtonCard.bringToFront()
                binding.topLayout.bringToFront()
            }
            
            // Make sure it's clickable and focusable
            locationButtonCard.isClickable = true
            locationButtonCard.isFocusable = true
            locationButtonCard.isFocusableInTouchMode = true
            locationButtonCard.isEnabled = true
            
            // Bring to front to ensure it's above other views (especially navigation buttons)
            locationButtonCard.bringToFront()
            binding.topLayout.bringToFront()
            
            // Force elevation and translationZ to be on top
            locationButtonCard.elevation = 50f
            locationButtonCard.translationZ = 50f
            
            // Make parent views non-clickable to prevent interception
            val parent = locationButtonCard.parent as? android.view.ViewGroup
            parent?.isClickable = false
            parent?.isFocusable = false
            
            // Create a click handler function
            val clickHandler = {
                android.util.Log.d("StoryPagerAdapter", "*** Location button clicked: $placeName at ($lat, $lng) ***")
                android.util.Log.d("StoryPagerAdapter", "*** Button click handler executing - opening maps ***")
                try {
                    // Pause story while opening maps
                    pauseStory(binding)
                    android.util.Log.d("StoryPagerAdapter", "Story paused, opening Google Maps...")
                    // Open location in Google Maps
                    openLocationInMaps(placeName, lat, lng)
                    android.util.Log.d("StoryPagerAdapter", "Google Maps intent sent successfully")
                } catch (e: Exception) {
                    android.util.Log.e("StoryPagerAdapter", "ERROR in location click handler: ${e.message}", e)
                    e.printStackTrace()
                }
            }
            
            // Set click listener on the card
            locationButtonCard.setOnClickListener {
                android.util.Log.d("StoryPagerAdapter", "onClickListener triggered")
                clickHandler()
            }
            
            // Also make the inner LinearLayout non-clickable so card handles it
            val locationLayout = locationButtonCard.getChildAt(0) as? android.view.ViewGroup
            locationLayout?.isClickable = false
            locationLayout?.isFocusable = false
            
            // Handle touch events to prevent parent views from intercepting
            locationButtonCard.setOnTouchListener { view, event ->
                val x = event.rawX.toInt()
                val y = event.rawY.toInt()
                
                // CRITICAL: Always consume ALL touch events to prevent navigation buttons and ViewPager2 from receiving them
                when (event.action) {
                    android.view.MotionEvent.ACTION_DOWN -> {
                        android.util.Log.d("StoryPagerAdapter", "Location button touch DOWN at ($x, $y)")
                        // Prevent ViewPager2 from intercepting touches
                        viewPager.isUserInputEnabled = false
                        // Prevent ALL parent views from intercepting touches
                        var parent = view.parent
                        while (parent != null) {
                            if (parent is android.view.ViewGroup) {
                                parent.requestDisallowInterceptTouchEvent(true)
                            }
                            // Specifically handle ViewPager2's RecyclerView
                            if (parent is androidx.recyclerview.widget.RecyclerView) {
                                parent.requestDisallowInterceptTouchEvent(true)
                            }
                            parent = parent.parent
                        }
                        // Disable navigation buttons temporarily to prevent their click listeners from firing
                        binding.btnNext?.isEnabled = false
                        binding.btnNext?.isClickable = false
                        binding.btnPrevious?.isEnabled = false
                        binding.btnPrevious?.isClickable = false
                        view.isPressed = true
                        view.alpha = 0.8f
                        // CRITICAL: Return true to consume the event and prevent it from reaching navigation buttons
                        return@setOnTouchListener true
                    }
                    android.view.MotionEvent.ACTION_UP -> {
                        android.util.Log.d("StoryPagerAdapter", "Location button touch UP at ($x, $y)")
                        view.isPressed = false
                        view.alpha = 1.0f
                        // Execute the click FIRST before re-enabling navigation buttons
                        android.util.Log.d("StoryPagerAdapter", "Executing location click handler from touch UP")
                        clickHandler()
                        // Re-enable ViewPager2
                        viewPager.isUserInputEnabled = true
                        // Re-enable navigation buttons AFTER handling the click
                        binding.btnNext?.isEnabled = true
                        binding.btnNext?.isClickable = true
                        binding.btnPrevious?.isEnabled = true
                        binding.btnPrevious?.isClickable = true
                        // Allow parent views to intercept again
                        var parent = view.parent
                        while (parent != null) {
                            if (parent is android.view.ViewGroup) {
                                parent.requestDisallowInterceptTouchEvent(false)
                            }
                            if (parent is androidx.recyclerview.widget.RecyclerView) {
                                parent.requestDisallowInterceptTouchEvent(false)
                            }
                            parent = parent.parent
                        }
                        // CRITICAL: Return true to consume the event and prevent navigation button clicks
                        return@setOnTouchListener true
                    }
                    android.view.MotionEvent.ACTION_CANCEL -> {
                        android.util.Log.d("StoryPagerAdapter", "Location button touch CANCEL")
                        view.isPressed = false
                        view.alpha = 1.0f
                        // Re-enable ViewPager2
                        viewPager.isUserInputEnabled = true
                        binding.btnNext?.isEnabled = true
                        binding.btnNext?.isClickable = true
                        binding.btnPrevious?.isEnabled = true
                        binding.btnPrevious?.isClickable = true
                        var parent = view.parent
                        while (parent != null) {
                            if (parent is android.view.ViewGroup) {
                                parent.requestDisallowInterceptTouchEvent(false)
                            }
                            if (parent is androidx.recyclerview.widget.RecyclerView) {
                                parent.requestDisallowInterceptTouchEvent(false)
                            }
                            parent = parent.parent
                        }
                        // CRITICAL: Return true to consume cancel event
                        return@setOnTouchListener true
                    }
                    android.view.MotionEvent.ACTION_MOVE -> {
                        // Keep preventing interception during move
                        var parent = view.parent
                        while (parent != null) {
                            if (parent is android.view.ViewGroup) {
                                parent.requestDisallowInterceptTouchEvent(true)
                            }
                            if (parent is androidx.recyclerview.widget.RecyclerView) {
                                parent.requestDisallowInterceptTouchEvent(true)
                            }
                            parent = parent.parent
                        }
                        // CRITICAL: Return true to consume move events
                        return@setOnTouchListener true
                    }
                    else -> {
                        // For other events, still prevent interception
                        var parent = view.parent
                        while (parent != null) {
                            if (parent is android.view.ViewGroup) {
                                parent.requestDisallowInterceptTouchEvent(true)
                            }
                            if (parent is androidx.recyclerview.widget.RecyclerView) {
                                parent.requestDisallowInterceptTouchEvent(true)
                            }
                            parent = parent.parent
                        }
                        // CRITICAL: Return true to consume all events
                        return@setOnTouchListener true
                    }
                }
            }
            
            // Also add a long click listener as backup
            locationButtonCard.setOnLongClickListener {
                android.util.Log.d("StoryPagerAdapter", "Location button long clicked")
                clickHandler()
                true
            }
            
            // Test: Add a simple test click to verify button is working
            android.util.Log.d("StoryPagerAdapter", "Location button setup complete. Button should be clickable now.")
        } else {
            android.util.Log.e("StoryPagerAdapter", "Location button views not found! Card: ${locationButtonCard != null}, Text: ${locationButtonText != null}")
        }
    }
    
    private fun openLocationInMaps(placeName: String, lat: Double, lng: Double) {
        android.util.Log.d("StoryPagerAdapter", "openLocationInMaps called with placeName=$placeName, lat=$lat, lng=$lng")
        
        try {
            // First, try to open Google Maps app directly
            val gmmIntentUri = Uri.parse("geo:$lat,$lng?q=$lat,$lng(${Uri.encode(placeName)})")
            android.util.Log.d("StoryPagerAdapter", "Created geo URI: $gmmIntentUri")
            
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
            mapIntent.setPackage("com.google.android.apps.maps")
            
            // Check if Google Maps is available
            val resolveInfo = context.packageManager.resolveActivity(mapIntent, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)
            android.util.Log.d("StoryPagerAdapter", "Google Maps resolveActivity result: $resolveInfo")
            
            if (resolveInfo != null) {
                android.util.Log.d("StoryPagerAdapter", "Starting Google Maps activity...")
                context.startActivity(mapIntent)
                android.util.Log.d("StoryPagerAdapter", "Google Maps activity started successfully")
                return
            }
        } catch (e: android.content.ActivityNotFoundException) {
            android.util.Log.d("StoryPagerAdapter", "Google Maps app not found, trying alternatives...")
        } catch (e: Exception) {
            android.util.Log.e("StoryPagerAdapter", "Error opening Google Maps app: ${e.message}", e)
        }
        
        // Fallback 1: Try Google Maps web URL
        try {
            android.util.Log.d("StoryPagerAdapter", "Trying Google Maps web URL...")
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=$lat,$lng"))
            webIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(webIntent)
            android.util.Log.d("StoryPagerAdapter", "Google Maps web URL opened successfully")
            return
        } catch (e: Exception) {
            android.util.Log.e("StoryPagerAdapter", "Error opening Google Maps web URL: ${e.message}", e)
        }
        
        // Fallback 2: Try generic geo intent (opens any map app)
        try {
            android.util.Log.d("StoryPagerAdapter", "Trying generic geo intent...")
            val geoIntent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:$lat,$lng?q=${Uri.encode(placeName)}"))
            geoIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(geoIntent)
            android.util.Log.d("StoryPagerAdapter", "Generic geo intent opened successfully")
            return
        } catch (e: Exception) {
            android.util.Log.e("StoryPagerAdapter", "Error opening geo intent: ${e.message}", e)
        }
        
        // Last resort: Show error message
        android.util.Log.e("StoryPagerAdapter", "All map opening methods failed. No map app available.")
        android.widget.Toast.makeText(context, "Unable to open maps. Please install a map application.", android.widget.Toast.LENGTH_SHORT).show()
    }

    private fun setupTaggedPeopleLinks(binding: StoryScreenLayoutBinding, taggedRef: List<TaggedRef>) {
        if (taggedRef.isEmpty()) {
            binding.taggedPeopleTv.visibility = View.GONE
            binding.taggedPeopleTv.text = ""
            binding.taggedPeopleTv.movementMethod = null
            return
        }

        val baseText = StringBuilder("With ")
        val spans = mutableListOf<TagSpanInfo>()

        taggedRef.forEachIndexed { index, tagged ->
            val name = tagged.name ?: return@forEachIndexed
            val userId = tagged.Id ?: return@forEachIndexed

            val start = baseText.length
            baseText.append(name)
            val end = baseText.length

            spans.add(TagSpanInfo(start, end, userId))

            if (index < taggedRef.size - 1) {
                baseText.append(if (index == taggedRef.size - 2) " and " else ", ")
            }
        }

        val fullText = baseText.toString()
        val spannable = SpannableString(fullText)
        val linkColor = ContextCompat.getColor(context, R.color.blue)

        spans.forEach { info ->
            // Clickable span to open profile
            val clickableSpan = object : ClickableSpan() {
                override fun onClick(widget: View) {
                    moveToProfileDetailsActivity(info.userId)
                }

                override fun updateDrawState(ds: TextPaint) {
                    super.updateDrawState(ds)
                    ds.color = linkColor
                    ds.isUnderlineText = false
                }
            }
            spannable.setSpan(
                clickableSpan,
                info.start,
                info.end,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            // Explicitly color the link text blue
            spannable.setSpan(
                ForegroundColorSpan(linkColor),
                info.start,
                info.end,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        binding.taggedPeopleTv.visibility = View.VISIBLE
        binding.taggedPeopleTv.text = spannable
        binding.taggedPeopleTv.movementMethod = LinkMovementMethod.getInstance()
        // Ensure the TextView can receive click events for links
        binding.taggedPeopleTv.highlightColor = android.graphics.Color.TRANSPARENT

        // Safety fallback: if link clicks are intercepted by parent views,
        // still open the first tagged user's profile when the line is tapped.
        val firstUserId = taggedRef.firstOrNull()?.Id
        if (!firstUserId.isNullOrEmpty()) {
            binding.taggedPeopleTv.setOnClickListener {
                moveToProfileDetailsActivity(firstUserId)
            }
        } else {
            binding.taggedPeopleTv.setOnClickListener(null)
        }
    }

    private fun showBottomSheet(storyId: String, binding: StoryScreenLayoutBinding) {
        pauseStory(binding)
        // Show the BottomSheetFragment
        val bottomSheet = YesOrNoBottomSheetFragment.newInstance(MessageStore.sureWantToDeleteStory(context))
        bottomSheet.onYesClicked = {
            // Handle Yes button click
            individualViewModal.deleteStory(storyId)
        }
        bottomSheet.onNoClicked = {
            resumeStory(binding)
        }
        bottomSheet.show(supportFragmentManager, "YesOrNoBottomSheet")
    }

    private fun viewStory(storyId: String) {
        if (storyId.isNotEmpty()) {
            val currentUserId = preferenceManager.getString(PreferenceManager.Keys.USER_ID, "") ?: ""
            // Mark story as viewed locally for immediate UI update
            com.thehotelmedia.android.customClasses.ViewedStoriesManager.markStoryAsViewed(
                context,
                currentUserId,
                storyId
            )
            // Also notify backend
            individualViewModal.viewStory(storyId)
        }
    }

    private fun likeUserStory(storyId: String) {
        individualViewModal.likeStory(storyId)
    }
    


    private fun resetProgressBars(count: Int, binding: StoryScreenLayoutBinding) {
        // Cancel any running animations
        currentAnimator?.cancel()
        currentAnimator = null

        // Remove all views from the progress container and clear the progress bar list
        binding.storyProgressContainer.removeAllViews()
        progressBars.clear()

        // Initialize new progress bars for the current user's stories
        initProgressBars(count, binding)
    }

    private fun initProgressBars(count: Int, binding: StoryScreenLayoutBinding) {
        // Initialize progress bars for the number of stories the user has
        for (i in 0 until count) {
            val progressBar = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
                max = 1000
                layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginEnd = 8
                    marginStart = 8
                }
                progressDrawable = ContextCompat.getDrawable(context, R.drawable.progress_bar_drawable)
            }
            binding.storyProgressContainer.addView(progressBar)
            progressBars.add(progressBar)
        }
    }

    private fun stopCurrentStory() {
        // Cancel any running animations
        currentAnimator?.cancel()
        currentAnimator = null

        // Stop video playback if applicable
        if (isVideoPlaying) {
            currentExoPlayer?.stop()
            currentExoPlayer?.release()
            currentExoPlayer = null
            isVideoPlaying = false
        }
    }

    private fun showCurrentStory(
        storyMedia: ArrayList<StoriesRef>,
        binding: StoryScreenLayoutBinding,
        users: Stories
    ) {
        if (currentStoryIndex < storyMedia.size) {
            // Immediately fill all previous progress bars
            for (i in 0 until currentStoryIndex) {
                progressBars[i].progress = 1000
            }

            val mediaDuration: Int
            val currentProgressBar = progressBars[currentStoryIndex]
            currentProgressBar.progress = 0

            val mediaUrl = storyMedia[currentStoryIndex].sourceUrl ?: ""
            if (isVideo(mediaUrl)) {
                // Start with default duration, will update when video loads
                mediaDuration = 15_000
                playVideo(mediaUrl, binding, users)
            } else {
                mediaDuration = 15_000 // Fixed 15 seconds for images
                showImage(mediaUrl, mediaDuration.toLong(), binding,users)
            }
        }
    }

    // Function to calculate video duration asynchronously
    private suspend fun getVideoDurationAsync(videoUrl: String): Int = withContext(Dispatchers.IO) {
        val player = ExoPlayer.Builder(context).build()
        return@withContext try {
            val mediaItem = MediaItem.fromUri(videoUrl)
            player.setMediaItem(mediaItem)
            player.prepare()
            val duration = player.duration
            if (duration > 0) duration.toInt() else 15_000 // Default to 15 seconds if duration unknown
        } catch (e: Exception) {
            Log.e("StoryPagerAdapter", "Error getting video duration: ${e.message}")
            15_000 // Default to 15 seconds on error
        } finally {
            player.release()
        }
    }

    private fun isVideo(url: String): Boolean {
        return url.endsWith(".mp4") || url.endsWith(".avi") || url.endsWith(".mov") || url.endsWith(".m3u8")
    }

    private fun showImage(
        imageUrl: String,
        duration: Long,
        binding: StoryScreenLayoutBinding,
        users: Stories
    ) {
        // Release any video player
        currentExoPlayer?.release()
        currentExoPlayer = null
        isVideoPlaying = false
        
        // Ensure the correct UI visibility for image
        binding.videoView?.visibility = View.GONE
        binding.root.findViewById<StyledPlayerView>(R.id.exoPlayerView)?.visibility = View.GONE
        binding.imageView.visibility = View.VISIBLE

        // Check if the context is still valid before loading the image
        if (!isActivityDestroyed()) {
            // Load the image using Glide, now using binding.root (which is the root view) as the context
            Glide.with(binding.root.context)  // This ties Glide to the view lifecycle
                .load(imageUrl)
                .placeholder(R.drawable.ic_post_placeholder)
                .transition(DrawableTransitionOptions.withCrossFade())  // Optional transition
                .into(binding.imageView)
        } else {
            // Skip loading if activity is destroyed or finishing
            Log.d("StoryPagerAdapter", "Activity is finishing or destroyed, skipping image load.")
        }

        // Animate the progress bar for the duration of the image
        animateProgressBar(duration, binding,users)
    }

    // Helper function to check if the activity is destroyed or finishing
    private fun isActivityDestroyed(): Boolean {
        return (context is Activity) && ((context as Activity).isFinishing || (context as Activity).isDestroyed)
    }

    private fun playVideo(
        videoUrl: String,
        binding: StoryScreenLayoutBinding,
        users: Stories
    ) {
        // Release previous player if exists
        currentExoPlayer?.release()
        currentExoPlayer = null

        // Ensure the correct UI visibility for video
        binding.imageView.visibility = View.GONE
        
        // Check if VideoView exists and hide it, or if StyledPlayerView exists
        try {
            binding.videoView?.visibility = View.GONE
        } catch (e: Exception) {
            // VideoView might not exist, that's okay
        }

        // Create ExoPlayer
        val exoPlayer = ExoPlayer.Builder(context).build().apply {
            val mediaItem = MediaItem.fromUri(videoUrl)
            setMediaItem(mediaItem)
            repeatMode = Player.REPEAT_MODE_ONE
            playWhenReady = true
            prepare()
            
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_READY -> {
                            isVideoPlaying = true
                            val duration = this@apply.duration
                            if (duration > 0) {
                                // Animate the progress bar for the actual video duration
                                animateProgressBar(duration, binding, users)
                            } else {
                                // Fallback to 15 seconds if duration unknown
                                animateProgressBar(15_000L, binding, users)
                            }
                        }
                        Player.STATE_BUFFERING -> {
                            currentAnimator?.pause()
                        }
                        Player.STATE_ENDED -> {
                            moveToNextStory(binding, users)
                            isVideoPlaying = false
                        }
                    }
                }
            })
        }
        
        currentExoPlayer = exoPlayer
        
        // Try to use StyledPlayerView if it exists, otherwise fall back to VideoView
        try {
            val playerView = binding.root.findViewById<StyledPlayerView>(R.id.exoPlayerView)
            if (playerView != null) {
                playerView.player = exoPlayer
                playerView.visibility = View.VISIBLE
            } else {
                // Fallback: create and add StyledPlayerView dynamically
                val styledPlayerView = StyledPlayerView(context).apply {
                    id = R.id.exoPlayerView
                    layoutParams = android.view.ViewGroup.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    useController = false
                    resizeMode = com.google.android.exoplayer2.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
                styledPlayerView.player = exoPlayer
                (binding.root as? android.view.ViewGroup)?.addView(styledPlayerView, 0)
            }
        } catch (e: Exception) {
            Log.e("StoryPagerAdapter", "Error setting up ExoPlayer: ${e.message}")
            // Fallback to VideoView if ExoPlayer setup fails
            binding.videoView?.let { videoView ->
                videoView.visibility = View.VISIBLE
                val uri = Uri.parse(videoUrl)
                videoView.setVideoURI(uri)
                videoView.setOnPreparedListener { mediaPlayer ->
                    mediaPlayer.start()
                    isVideoPlaying = true
                    animateProgressBar(mediaPlayer.duration.toLong(), binding, users)
                    mediaPlayer.setOnCompletionListener {
                        moveToNextStory(binding, users)
                        isVideoPlaying = false
                    }
                }
            }
        }
    }


    private fun animateProgressBar(
        duration: Long,
        binding: StoryScreenLayoutBinding,
        users: Stories
    ) {
        // Animate the progress bar based on the duration of the story
        val currentProgressBar = progressBars[currentStoryIndex]
        currentAnimator = ValueAnimator.ofInt(0, 1000).apply {
            this.duration = duration
            interpolator = LinearInterpolator()

            addUpdateListener { animation ->
                val progress = animation.animatedValue as Int
                currentProgressBar.progress = progress

                if (progress == 1000) {
                    moveToNextStory(binding, users)
                }
            }

            start()
        }
    }

    private fun moveToNextStory(binding: StoryScreenLayoutBinding, users: Stories) {
        currentStoryIndex++

        if (currentStoryIndex < progressBars.size) {
            // Show the next story
            val storyMedia = userList[previousPageIndex].storiesRef
            showCurrentStory(storyMedia, binding, users)
        } else {
            // All stories for this user are done
            // Mark all stories for this user as viewed in SharedPreferences (scoped per viewer)
            val allStoryIds = users.storiesRef.mapNotNull { it.Id }.filter { it.isNotEmpty() }
            if (allStoryIds.isNotEmpty()) {
                val currentUserId = preferenceManager.getString(PreferenceManager.Keys.USER_ID, "") ?: ""
                com.thehotelmedia.android.customClasses.ViewedStoriesManager.markAllStoriesAsViewed(
                    context,
                    currentUserId,
                    allStoryIds
                )
                android.util.Log.d("StoryPagerAdapter", "Marked all ${allStoryIds.size} stories as viewed for user ${users.id}")
            }
            
            // Optionally handle the transition to the next user or stop the story sequence
            currentStoryIndex = 0  // Reset for next user

            if (previousPageIndex + 1 < userList.size) {
                // Move to the next user (next page)
                viewPager.setCurrentItem(previousPageIndex + 1, true)
            } else {
                // Optionally loop back to the first user if needed
//                viewPager.setCurrentItem(0, true)
//                Toast.makeText(context, "All users' stories finished", Toast.LENGTH_SHORT).show()
                moveToMainScreen()
            }
        }

        notifyItemChanged(previousPageIndex)
    }

    override fun getItemCount(): Int {
        return userList.size
    }

    private fun setClickListener(binding: StoryScreenLayoutBinding, users: Stories) {
        
        binding.btnNext.setOnClickListener { view ->
            // Final safety check: verify touch is NOT on location button
            val locationButtonCard = binding.root.findViewById<com.google.android.material.card.MaterialCardView>(R.id.locationButtonCard)
            if (locationButtonCard != null && locationButtonCard.visibility == View.VISIBLE) {
                // Get the last touch coordinates from the view
                val locationButtonLocation = IntArray(2)
                locationButtonCard.getLocationOnScreen(locationButtonLocation)
                val viewLocation = IntArray(2)
                view.getLocationOnScreen(viewLocation)
                
                // Estimate touch position (center of view for click events)
                val touchX = viewLocation[0] + view.width / 2
                val touchY = viewLocation[1] + view.height / 2
                
                val left = locationButtonLocation[0]
                val top = locationButtonLocation[1]
                val right = left + locationButtonCard.width
                val bottom = top + locationButtonCard.height
                
                if (touchX >= left && touchX <= right && touchY >= top && touchY <= bottom) {
                    android.util.Log.d("StoryPagerAdapter", "btnNext onClick: touch is on location button - ignoring")
                    return@setOnClickListener // Don't execute navigation
                }
            }
            
            android.util.Log.d("StoryPagerAdapter", "btnNext onClick triggered")
            
            // Fill the progress bars of previous stories immediately
            if (currentStoryIndex < progressBars.size - 1) {
                // Stop the current progress bar animation
                progressBars[currentStoryIndex].progress = 1000
                currentAnimator?.cancel()
//                currentStoryIndex++
                if (currentStoryIndex < users.storiesRef.size - 1) {
                    currentStoryIndex++
                }

                // Show the next story (image/video) and animate the progress bar for the current story
                val storyMedia = userList[previousPageIndex].storiesRef  // Get media for the current user
                showCurrentStory(storyMedia, binding, users)

            } else {
                // If it's the last story of the current user, reset to the first story
                progressBars[currentStoryIndex].progress = 1000  // Fill the last progress bar
                currentStoryIndex = 0

                // If there are more users, move to the next user
                if (previousPageIndex + 1 < userList.size) {
                    viewPager.setCurrentItem(previousPageIndex + 1, true)
                } else {
                    // Optionally loop back to the first user or stop the sequence
                    // viewPager.setCurrentItem(0, true)  // Uncomment to loop

//                    Toast.makeText(context, "All users' stories finished", Toast.LENGTH_SHORT).show()
                    moveToMainScreen()
                }
            }
            notifyItemChanged(previousPageIndex)
        }

        binding.btnPrevious.setOnClickListener { view ->
            // Final safety check: verify touch is NOT on location button
            val locationButtonCard = binding.root.findViewById<com.google.android.material.card.MaterialCardView>(R.id.locationButtonCard)
            if (locationButtonCard != null && locationButtonCard.visibility == View.VISIBLE) {
                // Get the last touch coordinates from the view
                val locationButtonLocation = IntArray(2)
                locationButtonCard.getLocationOnScreen(locationButtonLocation)
                val viewLocation = IntArray(2)
                view.getLocationOnScreen(viewLocation)
                
                // Estimate touch position (center of view for click events)
                val touchX = viewLocation[0] + view.width / 2
                val touchY = viewLocation[1] + view.height / 2
                
                val left = locationButtonLocation[0]
                val top = locationButtonLocation[1]
                val right = left + locationButtonCard.width
                val bottom = top + locationButtonCard.height
                
                if (touchX >= left && touchX <= right && touchY >= top && touchY <= bottom) {
                    android.util.Log.d("StoryPagerAdapter", "btnPrevious onClick: touch is on location button - ignoring")
                    return@setOnClickListener // Don't execute navigation
                }
            }
            
            android.util.Log.d("StoryPagerAdapter", "btnPrevious onClick triggered")
            
            // If we're at the first story of the first user, do nothing
            if (previousPageIndex == 0 && currentStoryIndex == 0) {
                return@setOnClickListener  // Do nothing
            }
            // If we're at the first story of any other user, go to the previous user's first story
            if (currentStoryIndex == 0) {
                // Move to the previous user
                if (previousPageIndex - 1 >= 0) {
                    // Go to the previous user's first story
                    viewPager.setCurrentItem(previousPageIndex - 1, true)
                }
            } else {
                // If we are not at the first story of the user, move to the previous story within the same user
                progressBars[currentStoryIndex].progress = 0  // Fill the current progress bar
                currentAnimator?.pause()  // Stop the current progress animation
                currentStoryIndex--  // Move to the previous story
                // Show the previous story (image/video) and animate the progress bar for the current story
                val storyMedia = userList[previousPageIndex].storiesRef  // Get media for the current user
                showCurrentStory(storyMedia, binding, users)  // Display the previous story
            }
            notifyItemChanged(previousPageIndex)
        }

        // Combined touch listener for btnNext that checks location button AND handles long press
        binding.btnNext.setOnTouchListener { view, event ->
            // FIRST: Check if touch is on location button - this takes priority
            val locationButtonCard = binding.root.findViewById<com.google.android.material.card.MaterialCardView>(R.id.locationButtonCard)
            if (locationButtonCard != null && locationButtonCard.visibility == View.VISIBLE) {
                val location = IntArray(2)
                locationButtonCard.getLocationOnScreen(location)
                val x = event.rawX.toInt()
                val y = event.rawY.toInt()
                
                val left = location[0]
                val top = location[1]
                val right = left + locationButtonCard.width
                val bottom = top + locationButtonCard.height
                
                if (x >= left && x <= right && y >= top && y <= bottom) {
                    // Touch is on location button - CONSUME the event
                    android.util.Log.d("StoryPagerAdapter", "btnNext touch on location button - consuming")
                    return@setOnTouchListener true // Consume to prevent click listener
                }
            }
            
            // SECOND: Handle long press pause/resume
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // Start long press timer (Android handles this automatically)
                }
                MotionEvent.ACTION_UP -> {
                    // Resume the story when touch is released
                    resumeStory(binding)
                }
            }
            false // Don't consume, let click listener handle it if not on location button
        }
        
        binding.btnNext.setOnLongClickListener {
            // Handle long press: pause the story
            pauseStory(binding)
            true // Consume the long press event
        }

        // Combined touch listener for btnPrevious that checks location button AND handles long press
        binding.btnPrevious.setOnTouchListener { view, event ->
            // FIRST: Check if touch is on location button - this takes priority
            val locationButtonCard = binding.root.findViewById<com.google.android.material.card.MaterialCardView>(R.id.locationButtonCard)
            if (locationButtonCard != null && locationButtonCard.visibility == View.VISIBLE) {
                val location = IntArray(2)
                locationButtonCard.getLocationOnScreen(location)
                val x = event.rawX.toInt()
                val y = event.rawY.toInt()
                
                val left = location[0]
                val top = location[1]
                val right = left + locationButtonCard.width
                val bottom = top + locationButtonCard.height
                
                if (x >= left && x <= right && y >= top && y <= bottom) {
                    // Touch is on location button - CONSUME the event
                    android.util.Log.d("StoryPagerAdapter", "btnPrevious touch on location button - consuming")
                    return@setOnTouchListener true // Consume to prevent click listener
                }
            }
            
            // SECOND: Handle long press pause/resume
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // Start long press timer (Android handles this automatically)
                }
                MotionEvent.ACTION_UP -> {
                    // Resume the story when touch is released
                    resumeStory(binding)
                }
            }
            false // Don't consume, let click listener handle it if not on location button
        }
        
        binding.btnPrevious.setOnLongClickListener {
            // Handle long press: pause the story
            pauseStory(binding)
            true // Consume the long press event
        }
    }

    private fun pauseStory(binding: StoryScreenLayoutBinding) {
        binding.storyProgressContainer.visibility = View.GONE
        binding.topLayout.visibility = View.GONE
        binding.bottomLayout.visibility = View.GONE
        onItemClicked(true)
        // Pause the current progress bar animation
        currentAnimator?.pause()

        // Pause video if it's playing
        if (isVideoPlaying) {
            currentExoPlayer?.pause()
            try {
                binding.videoView?.pause()
            } catch (e: Exception) {
                // VideoView might not exist
            }
        }
    }

    private fun resumeStory(binding: StoryScreenLayoutBinding) {
        binding.storyProgressContainer.visibility = View.VISIBLE
        binding.topLayout.visibility = View.VISIBLE
        binding.bottomLayout.visibility = View.VISIBLE
        onItemClicked(false)
        // Resume the progress bar animation
        currentAnimator?.resume()

        // Resume video if it's playing
        if (isVideoPlaying) {
            currentExoPlayer?.play()
            try {
                binding.videoView?.start()
            } catch (e: Exception) {
                // VideoView might not exist
            }
        }
    }

}
