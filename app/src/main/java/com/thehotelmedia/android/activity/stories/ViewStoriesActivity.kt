package com.thehotelmedia.android.activity.stories

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.widget.ViewPager2
import com.google.gson.Gson
import com.thehotelmedia.android.Socket.SocketViewModel
import com.thehotelmedia.android.ViewModelFactory
import com.thehotelmedia.android.activity.BaseActivity
import com.thehotelmedia.android.activity.userTypes.business.bottomNavigation.BottomNavigationBusinessMainActivity
import com.thehotelmedia.android.activity.userTypes.individual.bottomNavigation.BottomNavigationIndividualMainActivity
import com.thehotelmedia.android.adapters.NotificationAdapter
import com.thehotelmedia.android.customClasses.Constants.business_type_individual
import com.thehotelmedia.android.customClasses.CustomProgressBar
import com.thehotelmedia.android.customClasses.CustomSnackBar
import com.thehotelmedia.android.customClasses.PreferenceManager
import com.thehotelmedia.android.databinding.ActivityViewStoriesBinding
import com.thehotelmedia.android.modals.Stories.Stories
import com.thehotelmedia.android.repository.IndividualRepo
import com.thehotelmedia.android.viewModal.individualViewModal.IndividualViewModal

class ViewStoriesActivity : BaseActivity() {

    private lateinit var binding : ActivityViewStoriesBinding

    private lateinit var progressBar : CustomProgressBar
    private lateinit var individualViewModal: IndividualViewModal
    private lateinit var preferenceManager : PreferenceManager
    private lateinit var storyPagerAdapter: StoryPagerAdapter
    private val socketViewModel: SocketViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewStoriesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initUi()

        // Handle back button with OnBackPressedDispatcher
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Release players before navigating away
                if (::storyPagerAdapter.isInitialized) {
                    storyPagerAdapter.releaseAllPlayers()
                }
                storyPagerAdapter.moveToMainScreen()
                // You can perform additional actions here if needed.
                // Call finish() if you want to close the activity:
                // finish()
            }
        })
    }



    private fun initUi() {



        preferenceManager = PreferenceManager.getInstance(this)
        val individualRepo = IndividualRepo(this)
        individualViewModal = ViewModelProvider(this, ViewModelFactory(null,individualRepo,null))[IndividualViewModal::class.java]
        progressBar = CustomProgressBar(this)
        val businessType = preferenceManager.getString(PreferenceManager.Keys.BUSINESS_TYPE, "").toString()
// Convert JSON string back to list
        val jsonString = intent.getStringExtra("StoriesJson")
        val myUserName = preferenceManager.getString(PreferenceManager.Keys.USER_USER_NAME, "").orEmpty()
        val userID = preferenceManager.getString(PreferenceManager.Keys.USER_ID, "").orEmpty()

        socketViewModel.connectSocket(myUserName, userID)
        // Check if the JSON string exists
        if (jsonString != null) {
            // Convert the JSON string back to a Stories object
//            val story = Gson().fromJson(jsonString, Stories::class.java)
            val storiesList = Gson().fromJson(jsonString, Array<Stories>::class.java).toList()
            println("sadjfgajsg  Retrieved storiesList: $storiesList")
            
            // DEBUG: Log user tagging data from deserialized stories
            android.util.Log.d("ViewStoriesActivity", "=== DESERIALIZED STORIES DEBUG ===")
            android.util.Log.d("ViewStoriesActivity", "Total stories: ${storiesList.size}")
            storiesList.forEachIndexed { userIndex, stories ->
                android.util.Log.d("ViewStoriesActivity", "User[$userIndex] - id: ${stories.id}, name: ${stories.name}, storiesRef count: ${stories.storiesRef.size}")
                stories.storiesRef.forEachIndexed { storyIndex, storyRef ->
                    android.util.Log.d("ViewStoriesActivity", "  Story[$storyIndex] ID: ${storyRef.Id}")
                    android.util.Log.d("ViewStoriesActivity", "    userTaggedName: '${storyRef.userTaggedName}', userTaggedId: '${storyRef.userTaggedId}'")
                    android.util.Log.d("ViewStoriesActivity", "    userTaggedPositionX: ${storyRef.userTaggedPositionX}, userTaggedPositionY: ${storyRef.userTaggedPositionY}")
                    android.util.Log.d("ViewStoriesActivity", "    taggedUsers array size: ${storyRef.taggedUsers?.size ?: 0}")
                }
            }
            android.util.Log.d("ViewStoriesActivity", "===================================")

            // Ensure stories inside each user are ordered from oldest to newest
            val chronologicallySortedStories = storiesList.map { stories ->
                val sortedStoriesRef = stories.storiesRef
                    .sortedBy { it.createdAt ?: "" }
                stories.copy(storiesRef = ArrayList(sortedStoriesRef))
            }

            setViewPager(chronologicallySortedStories)
            // Now, you can access the story object data and bind it to your views
            // Example: binding.textViewStoryName.text = story.name
        } else {
            println("sadjfgajsg  No StoriesJson found in the intent")

        }



        individualViewModal.deleteStoryResult.observe(this){result->
            if (result.status == true){
                val msg = result.message.toString()
                CustomSnackBar.showSnackBar(binding.root,msg)
                storyPagerAdapter.moveToMainScreen()
            }else{
                val msg = result.message.toString()
                CustomSnackBar.showSnackBar(binding.root,msg)
            }
        }

        individualViewModal.blockUserResult.observe(this){result->
            if (result.status == true){
                val msg = result.message.toString()
                CustomSnackBar.showSnackBar(binding.root,msg)

                storyPagerAdapter.moveToMainScreen()
//               onBackPressedDispatcher.onBackPressed()
//                moveToMainScreen(businessType)


            }else{
                val msg = result.message.toString()
                CustomSnackBar.showSnackBar(binding.root,msg)
            }
        }


        individualViewModal.loading.observe(this){
            if (it == true){
                progressBar.show() // To show the giff progress bar
            }else{
                progressBar.hide() // To hide the giff progress bar
            }
        }

        individualViewModal.toast.observe(this){
//            Toast.makeText(activity,it, Toast.LENGTH_SHORT).show()
            CustomSnackBar.showSnackBar(binding.root,it)
        }


    }

    private fun moveToMainScreen(businessType: String) {
        if (businessType == business_type_individual) {
            val intent = Intent(this, BottomNavigationIndividualMainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
//                    finish()
        } else {
            val intent = Intent(this, BottomNavigationBusinessMainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
//                    finish()
        }
    }

    private fun setViewPager(storiesList: List<Stories>) {
        // Set up the adapter with the list of images
        storyPagerAdapter = StoryPagerAdapter(this,storiesList,binding.viewPager,preferenceManager,individualViewModal,supportFragmentManager,::hideNameLayout,socketViewModel)
        
        // Configure ViewPager2 for smooth swipe navigation between users
        binding.viewPager.orientation = ViewPager2.ORIENTATION_HORIZONTAL
        binding.viewPager.isUserInputEnabled = true // Enable swipe gestures
        binding.viewPager.offscreenPageLimit = 1 // Preload adjacent pages for smoother transitions
        
        // Set page transformer for smooth transitions
        binding.viewPager.setPageTransformer(RotateDownTransformer())
        
        binding.viewPager.adapter = storyPagerAdapter

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                // Page change is handled in StoryPagerAdapter
            }
        })
    }

    private fun hideNameLayout(action: Boolean) {
        if (action){

        }else{

        }


    }

    override fun onPause() {
        super.onPause()
        // Release ExoPlayer when activity is paused to stop audio
        if (::storyPagerAdapter.isInitialized) {
            storyPagerAdapter.releaseAllPlayers()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Release ExoPlayer when activity is destroyed
        if (::storyPagerAdapter.isInitialized) {
            storyPagerAdapter.releaseAllPlayers()
        }
    }

    override fun onStop() {
        super.onStop()
        // Release ExoPlayer when activity is stopped
        if (::storyPagerAdapter.isInitialized) {
            storyPagerAdapter.releaseAllPlayers()
        }
    }


}
class RotateDownTransformer : ViewPager2.PageTransformer {
    override fun transformPage(view: View, position: Float) {
        val rotation = 10f * position // Rotate the view down based on position

        view.pivotX = view.width * 0.5f
        view.pivotY = view.height.toFloat() // Rotate from the bottom of the view

        view.rotation = rotation
    }
}




