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
            
            val taggedPeople = story.taggedRef ?: arrayListOf()
            // Hide the old text-based tagged people view (replaced with floating tag button)
                binding.taggedPeopleTv.visibility = View.GONE
                binding.taggedPeopleTv.text = ""
                binding.taggedPeopleTv.setOnClickListener(null)
            
            // Hide locationTv (the TextView), but show the floating button
            binding.locationTv.visibility = View.GONE
            binding.locationTv.text = ""
            binding.locationTv.setOnClickListener(null)
            binding.locationTv.setOnTouchListener(null)
            
            // Display location if it exists - use floating button
            val location = story.location
            
            // Show floating location button if we have valid location data
            if (location != null && location.placeName != null && location.placeName!!.isNotEmpty()) {
                val lat = location.lat
                val lng = location.lng
                // Get position from root level (locationPositionX/locationPositionY), not from location object
                val locationX = story.locationPositionX  // Normalized x position (0.0-1.0)
                val locationY = story.locationPositionY  // Normalized y position (0.0-1.0)
                
                android.util.Log.d("StoryPagerAdapter", "Location data - placeName: ${location.placeName}, lat: $lat, lng: $lng, x: $locationX, y: $locationY")
                
                if (lat != null && lng != null && lat.isFinite() && lng.isFinite()) {
                    // Show and setup the floating location button with saved position
                    setupLocationFloatingButton(binding, location.placeName!!, lat, lng, locationX, locationY)
                } else {
                    hideLocationViews(binding) // Hide button if coordinates are invalid
                }
            } else {
                hideLocationViews(binding) // Explicitly hide if no location data
            }
            
            // Display user tag if it exists - use floating button
            // Get first tagged user for the user tag button
            val firstTaggedUser = taggedPeople.firstOrNull()
            
            // Backend returns userTagged (name), but we may need ID from taggedRef
            val userTaggedName = story.userTaggedName ?: firstTaggedUser?.name
            val userTaggedX = story.userTaggedPositionX
            val userTaggedY = story.userTaggedPositionY
            
            // Determine the final user ID - prioritize story.userTaggedId, then firstTaggedUser.Id
            val finalUserId = story.userTaggedId 
                ?: firstTaggedUser?.Id
                ?: ""
            
            android.util.Log.d("StoryPagerAdapter", "User tag data - userTaggedName: $userTaggedName, story.userTaggedId: '${story.userTaggedId}', firstTaggedUser?.Id: '${firstTaggedUser?.Id}', finalUserId: '$finalUserId', userTaggedX: $userTaggedX, userTaggedY: $userTaggedY, taggedPeople count: ${taggedPeople.size}")
            
            // Show the tag if we have a name (ID may be missing for older stories, but we still show the tag)
            if (!userTaggedName.isNullOrEmpty()) {
                // Show the tag - click handler will handle missing ID gracefully
                android.util.Log.d("StoryPagerAdapter", "Showing user tag - name: $userTaggedName, id: '$finalUserId' (${if (finalUserId.isNotEmpty()) "with navigation" else "no ID - click will show toast"})")
                setupUserTagButton(binding, userTaggedName, finalUserId, userTaggedX, userTaggedY)
            } else if (taggedPeople.isNotEmpty() && firstTaggedUser != null) {
                // Fallback: If we have taggedPeople but no userTaggedName, use the first tagged user
                val name = firstTaggedUser.name
                val id = firstTaggedUser.Id ?: ""
                if (!name.isNullOrEmpty()) {
                    android.util.Log.d("StoryPagerAdapter", "Using first tagged user as fallback - name: $name, id: '$id'")
                    setupUserTagButton(binding, name, id, userTaggedX, userTaggedY)
                } else {
                    hideUserTagViews(binding)
                }
            } else {
                android.util.Log.d("StoryPagerAdapter", "No user tag data found - hiding user tag views")
                hideUserTagViews(binding)
            }
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
    
    private fun hideLocationViews(binding: StoryScreenLayoutBinding) {
        // Hide the TextView location (locationTv)
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
            val locationButtonText = binding.root.findViewById<android.widget.TextView>(R.id.locationButtonText)
            locationButtonText?.text = ""
        }
    }
    
    private fun setupLocationFloatingButton(binding: StoryScreenLayoutBinding, placeName: String, lat: Double, lng: Double, locationX: Float? = null, locationY: Float? = null) {
        val locationButtonCard = binding.root.findViewById<com.google.android.material.card.MaterialCardView>(R.id.locationButtonCard)
        val locationButtonText = binding.root.findViewById<android.widget.TextView>(R.id.locationButtonText)
        
        if (locationButtonCard != null && locationButtonText != null) {
            // Set visibility and text
            locationButtonCard.visibility = View.VISIBLE
            locationButtonText.text = placeName
            
            // Make the location tag view clickable and focusable
            locationButtonCard.isClickable = true
            locationButtonCard.isFocusable = true
            locationButtonCard.isFocusableInTouchMode = true
            
            // Ensure inner LinearLayout doesn't intercept clicks
            val locationLayout = locationButtonCard.getChildAt(0) as? android.view.ViewGroup
            locationLayout?.isClickable = false
            locationLayout?.isFocusable = false
            
            // Position the location tag at saved x/y coordinates, or use default position
            // Remove constraints to allow free positioning via x/y
            val layoutParams = locationButtonCard.layoutParams as? androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
            layoutParams?.let {
                it.leftToLeft = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET
                it.topToTop = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET
                it.startToStart = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET
                it.leftMargin = 0
                it.topMargin = 0
                locationButtonCard.layoutParams = it
            }
            
            binding.root.post {
                val parentWidth = binding.root.width.toFloat()
                val parentHeight = binding.root.height.toFloat()
                
                android.util.Log.d("StoryPagerAdapter", "Positioning location tag - parentWidth: $parentWidth, parentHeight: $parentHeight, locationX: $locationX, locationY: $locationY")
                
                if (parentWidth > 0 && parentHeight > 0) {
                    if (locationX != null && locationY != null) {
                        // Use saved position (normalized 0.0-1.0 converted to pixels)
                        val xPosition = locationX * parentWidth
                        val yPosition = locationY * parentHeight
                        locationButtonCard.x = xPosition
                        locationButtonCard.y = yPosition
                        android.util.Log.d("StoryPagerAdapter", "Positioned location tag at x: $xPosition, y: $yPosition (normalized: x=$locationX, y=$locationY)")
                    } else {
                        // Default position: top-left area (matching Create Story default)
                        locationButtonCard.x = parentWidth * 0.12f  // 12% from left
                        locationButtonCard.y = parentHeight * 0.20f  // 20% from top
                        android.util.Log.d("StoryPagerAdapter", "Using default position - x: ${locationButtonCard.x}, y: ${locationButtonCard.y} (locationX and locationY are null)")
                    }
                } else {
                    android.util.Log.w("StoryPagerAdapter", "Cannot position location tag - invalid parent dimensions (width: $parentWidth, height: $parentHeight)")
                }
            }
            
            // Bring to front to ensure it's above other views (image/video/navigation buttons)
            locationButtonCard.bringToFront()
            
            // Simple click listener - the location tag view is the ONLY element handling clicks
            locationButtonCard.setOnClickListener {
                pauseStory(binding)
                openLocationInMaps(placeName, lat, lng)
            }
            
            // Clear any previous touch listeners - we don't need them
            locationButtonCard.setOnTouchListener(null)
        }
    }
    
    private fun openLocationInMaps(placeName: String, lat: Double, lng: Double) {
        try {
            // First, try to open Google Maps app directly
            val gmmIntentUri = Uri.parse("geo:$lat,$lng?q=$lat,$lng(${Uri.encode(placeName)})")
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
            mapIntent.setPackage("com.google.android.apps.maps")
            
            // Check if Google Maps is available
            val resolveInfo = context.packageManager.resolveActivity(mapIntent, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)
            if (resolveInfo != null) {
                context.startActivity(mapIntent)
                return
            }
        } catch (e: android.content.ActivityNotFoundException) {
            // Continue to fallback
        } catch (e: Exception) {
            // Continue to fallback
        }
        
        // Fallback 1: Try Google Maps web URL
        try {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=$lat,$lng"))
            webIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(webIntent)
            return
        } catch (e: Exception) {
            // Continue to fallback
        }
        
        // Fallback 2: Try generic geo intent (opens any map app)
        try {
            val geoIntent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:$lat,$lng?q=${Uri.encode(placeName)}"))
            geoIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(geoIntent)
            return
        } catch (e: Exception) {
            // Show error message
        }
        
        // Last resort: Show error message
        android.widget.Toast.makeText(context, "Unable to open maps. Please install a map application.", android.widget.Toast.LENGTH_SHORT).show()
    }

    private fun hideUserTagViews(binding: StoryScreenLayoutBinding) {
        val userTagButtonCard = binding.root.findViewById<com.google.android.material.card.MaterialCardView>(R.id.userTagButtonCard)
        userTagButtonCard?.visibility = View.GONE
        userTagButtonCard?.setOnClickListener(null)
    }
    
    private fun setupUserTagButton(binding: StoryScreenLayoutBinding, userName: String, userId: String, userTaggedX: Float? = null, userTaggedY: Float? = null) {
        val userTagButtonCard = binding.root.findViewById<com.google.android.material.card.MaterialCardView>(R.id.userTagButtonCard)
        val userTagButtonText = binding.root.findViewById<android.widget.TextView>(R.id.userTagButtonText)
        
        if (userTagButtonCard != null && userTagButtonText != null) {
            // Set visibility and text (display as @username)
            userTagButtonCard.visibility = View.VISIBLE
            val displayName = if (userName.startsWith("@")) userName else "@$userName"
            userTagButtonText.text = displayName
            
            // Make the user tag view clickable and focusable
            userTagButtonCard.isClickable = true
            userTagButtonCard.isFocusable = true
            userTagButtonCard.isFocusableInTouchMode = true
            
            // Ensure inner LinearLayout doesn't intercept clicks
            val userTagLayout = userTagButtonCard.getChildAt(0) as? android.view.ViewGroup
            userTagLayout?.isClickable = false
            userTagLayout?.isFocusable = false
            
            // Position the user tag at saved x/y coordinates, or use default position
            // Remove constraints to allow free positioning via x/y
            val layoutParams = userTagButtonCard.layoutParams as? androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
            layoutParams?.let {
                it.leftToLeft = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET
                it.topToTop = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET
                it.startToStart = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET
                it.leftMargin = 0
                it.topMargin = 0
                userTagButtonCard.layoutParams = it
            }
            
            binding.root.post {
                val parentWidth = binding.root.width.toFloat()
                val parentHeight = binding.root.height.toFloat()
                
                android.util.Log.d("StoryPagerAdapter", "Positioning user tag - parentWidth: $parentWidth, parentHeight: $parentHeight, userTaggedX: $userTaggedX, userTaggedY: $userTaggedY")
                
                if (parentWidth > 0 && parentHeight > 0) {
                    if (userTaggedX != null && userTaggedY != null) {
                        // Use saved position (normalized 0.0-1.0 converted to pixels)
                        val xPosition = userTaggedX * parentWidth
                        val yPosition = userTaggedY * parentHeight
                        userTagButtonCard.x = xPosition
                        userTagButtonCard.y = yPosition
                        android.util.Log.d("StoryPagerAdapter", "Positioned user tag at x: $xPosition, y: $yPosition (normalized: x=$userTaggedX, y=$userTaggedY)")
                    } else {
                        // Default position: top-left area (matching Create Story default)
                        userTagButtonCard.x = parentWidth * 0.25f  // 25% from left
                        userTagButtonCard.y = parentHeight * 0.25f  // 25% from top
                        android.util.Log.d("StoryPagerAdapter", "Using default position for user tag - x: ${userTagButtonCard.x}, y: ${userTagButtonCard.y} (userTaggedX and userTaggedY are null)")
                    }
                } else {
                    android.util.Log.w("StoryPagerAdapter", "Cannot position user tag - invalid parent dimensions (width: $parentWidth, height: $parentHeight)")
                }
            }
            
            // Bring to front to ensure it's above other views
            userTagButtonCard.bringToFront()
            
            // Set click listener to navigate to user profile
            userTagButtonCard.setOnClickListener {
                pauseStory(binding)
                if (userId.isNotEmpty()) {
                    moveToProfileDetailsActivity(userId)
                } else {
                    android.util.Log.w("StoryPagerAdapter", "Cannot navigate to profile - user ID is missing for user: $userName")
                    android.widget.Toast.makeText(context, "Unable to open profile. User ID not available.", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
            userTagButtonCard.setOnTouchListener(null)
        }
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
        binding.btnNext.setOnClickListener {
            // Fill the progress bars of previous stories immediately
            if (currentStoryIndex < progressBars.size - 1) {
                // Stop the current progress bar animation
                progressBars[currentStoryIndex].progress = 1000
                currentAnimator?.cancel()
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
                    moveToMainScreen()
                }
            }
            notifyItemChanged(previousPageIndex)
        }

        binding.btnNext.setOnLongClickListener {
            // Handle long press: pause the story
            pauseStory(binding)
            true // Consume the long press event
        }

        binding.btnPrevious.setOnClickListener {
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
