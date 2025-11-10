package com.thehotelmedia.android.activity.stories

import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
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
import com.thehotelmedia.android.viewModal.individualViewModal.IndividualViewModal

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
                val bottomSheet = YesOrNoBottomSheetFragment.newInstance("${getString(R.string.do_you_want_to_block)} $userName?")
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
            // Mark story as viewed locally for immediate UI update
            com.thehotelmedia.android.customClasses.ViewedStoriesManager.markStoryAsViewed(context, storyId)
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
                mediaDuration = getVideoDuration(mediaUrl)
                playVideo(mediaUrl, mediaDuration.toLong(), binding,users)
            } else {
                mediaDuration = 10_000 // Fixed 10 seconds for images
                showImage(mediaUrl, mediaDuration.toLong(), binding,users)
            }
        }
    }

    // Function to calculate video duration
    private fun getVideoDuration(videoUrl: String): Int {
        val mediaPlayer = MediaPlayer()
        return try {
            mediaPlayer.setDataSource(videoUrl)
            mediaPlayer.prepare() // Synchronously fetch metadata
            mediaPlayer.duration // Duration in milliseconds
        } catch (e: Exception) {
            0 // Return 0 if duration cannot be determined
        } finally {
            mediaPlayer.release()
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
        // Ensure the correct UI visibility for image
        binding.videoView.visibility = View.GONE
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
        duration: Long,
        binding: StoryScreenLayoutBinding,
        users: Stories
    ) {
        // Ensure the correct UI visibility for video
        binding.imageView.visibility = View.GONE
        binding.videoView.visibility = View.VISIBLE

        // Set video URI and start the video
        val uri = Uri.parse(videoUrl)
        binding.videoView.setVideoURI(uri)

        binding.videoView.setOnPreparedListener { mediaPlayer ->
            mediaPlayer.start()
            isVideoPlaying = true

            // Animate the progress bar for the video duration
            animateProgressBar(duration, binding, users)

            mediaPlayer.setOnCompletionListener {
                moveToNextStory(binding,users)
                isVideoPlaying = false
            }
        }

        // Listener for buffering events
        binding.videoView.setOnInfoListener { mediaPlayer, what, extra ->
            when (what) {
                MediaPlayer.MEDIA_INFO_BUFFERING_START -> {
//                    pauseStory(binding)
                    currentAnimator?.pause()
                }
                MediaPlayer.MEDIA_INFO_BUFFERING_END, MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START -> {
//                    resumeStory(binding)
                    currentAnimator?.resume()
                }
            }
            true
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
            // Mark all stories for this user as viewed in SharedPreferences
            val allStoryIds = users.storiesRef.mapNotNull { it.Id }.filter { it.isNotEmpty() }
            if (allStoryIds.isNotEmpty()) {
                com.thehotelmedia.android.customClasses.ViewedStoriesManager.markAllStoriesAsViewed(context, allStoryIds)
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

        binding.btnPrevious.setOnClickListener {
            // If we're at the first story of the first user, do nothing
            if (previousPageIndex == 0 && currentStoryIndex == 0) {
                return@setOnClickListener  // Do nothing
            }
            // If we're at the first story of any other user, go to the previous user's first story
            if (currentStoryIndex == 0) {
                // Move to the previous user
                if (previousPageIndex - 1 >= 0) {
                    // Go to the previous user’s first story
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

        binding.btnNext.setOnLongClickListener {
            // Handle long press: pause the story
            pauseStory(binding)
            true // Consume the long press event to prevent other actions
        }

        binding.btnNext.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_UP -> {
                    // Handle touch release: resume the story when the long press is released
                    resumeStory(binding)
                }
            }
            false // Don't consume the touch event, allowing other listeners to handle it
        }

        binding.btnPrevious.setOnLongClickListener {
            // Handle long press: pause the story
            pauseStory(binding)
            true // Consume the long press event to prevent other actions
        }

        binding.btnPrevious.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_UP -> {
                    // Handle touch release: resume the story when the long press is released
                    resumeStory(binding)
                }
            }
            false // Don't consume the touch event, allowing other listeners to handle it
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
            binding.videoView.pause()
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
            binding.videoView.start()
        }
    }

}
