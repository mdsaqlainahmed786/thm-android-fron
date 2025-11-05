package com.thehotelmedia.android.activity.userTypes.forms

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.bumptech.glide.Glide
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import com.thehotelmedia.android.R
import com.thehotelmedia.android.ViewModelFactory
import com.thehotelmedia.android.activity.BaseActivity
import com.thehotelmedia.android.activity.authentication.individual.IndividualMediaActivity.Companion.CAMERA_PERMISSION_CODE
import com.thehotelmedia.android.activity.userTypes.business.bottomNavigation.BottomNavigationBusinessMainActivity
import com.thehotelmedia.android.activity.userTypes.individual.bottomNavigation.BottomNavigationIndividualMainActivity
import com.thehotelmedia.android.activity.userTypes.forms.createPost.FeelingActivity
import com.thehotelmedia.android.activity.userTypes.forms.createPost.TagPeople
import com.thehotelmedia.android.activity.userTypes.forms.createPost.TagPeopleActivity
import com.thehotelmedia.android.adapters.userTypes.individual.forms.AttachedMediaAdapter
import com.thehotelmedia.android.adapters.userTypes.individual.forms.TagsAdapter
import com.thehotelmedia.android.customClasses.Constants
import com.thehotelmedia.android.customClasses.Constants.business_type_individual
import com.thehotelmedia.android.customClasses.CustomProgressBar
import com.thehotelmedia.android.customClasses.CustomSnackBar
import com.thehotelmedia.android.customClasses.GiffProgressBar
import com.thehotelmedia.android.customClasses.MessageStore
import com.thehotelmedia.android.customClasses.PreferenceManager
import com.thehotelmedia.android.customClasses.SuccessGiff
import com.thehotelmedia.android.customDialog.PhotoVideoDialog
import com.thehotelmedia.android.databinding.ActivityCreatePostBinding
import com.thehotelmedia.android.extensions.blurTheView
import com.thehotelmedia.android.extensions.navigateToMainActivity
import com.thehotelmedia.android.extensions.startCreatePostWorker
import com.thehotelmedia.android.repository.IndividualRepo
import com.thehotelmedia.android.viewModal.individualViewModal.IndividualViewModal
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


class CreatePostActivity : BaseActivity() {

    private lateinit var binding: ActivityCreatePostBinding
    private lateinit var attachedMediaAdapter: AttachedMediaAdapter
    private val mediaList = mutableListOf<String>()
    private var cameraImageUri: Uri? = null
    private val REQUEST_CODE_TAG_PEOPLE = 1001
    private var selectedTagPeopleList = ArrayList<TagPeople>()
    private var selectedCollaborators = ArrayList<TagPeople>()
    private var selectedFeeling: String = ""
    private var businessesType: String = ""
    private lateinit var preferenceManager : PreferenceManager
    private lateinit var checkInLauncher: ActivityResultLauncher<Intent>
    private val activity = this@CreatePostActivity
    private lateinit var individualViewModal: IndividualViewModal
    private var selectedPlaceName: String = ""
    private var selectedLat: Double = 0.0
    private var selectedLng: Double = 0.0
    private lateinit var progressBar: CustomProgressBar
    private lateinit var giffProgressBar: GiffProgressBar
    private lateinit var successGiff: SuccessGiff


    private val serviceReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Constants.CREATE_POST_BROADCAST) {
                navigateToMainActivity(businessesType == business_type_individual)
            }
        }
    }


    private var enabledMediaCount: Int = 10
    private val pickPhotosLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            if (contentResolver.getType(uri)?.startsWith("image/") == true) {

                val editIntent = Intent(this, EditImageActivity::class.java).apply {
                    putExtra("image_uri", uri)
                }
                editImageLauncher.launch(editIntent)

            }
        }
    }



    private val editImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val editedImageUri = result.data?.getStringExtra("edited_image_uri")
            editedImageUri?.let {
                mediaList.add(it)
                setMediaListAdapter()
            }
        }
    }

    // Define ActivityResultLauncher for picking a single video
    private val pickVideoLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            // Add the video to the mediaList
            checkVideoDurationAndTrim(uri)
//            mediaList.add(uri.toString())
//            setMediaListAdapter()
        }
    }


    private val cameraLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val imageBitmap = result.data?.extras?.get("data") as Bitmap
                val savedUri = saveImageToStorage(imageBitmap)
                savedUri?.let { uri ->
                    val editIntent = Intent(this, EditImageActivity::class.java).apply {
                        putExtra("image_uri", uri)
                    }
                    editImageLauncher.launch(editIntent)

                }
            }
        }


    private val videoTrimmerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val trimmedVideoUri = result.data?.getStringExtra("trimmed_video_uri")
            trimmedVideoUri?.let {
                mediaList.add(it)
                setMediaListAdapter()
            }
        }
    }

    private lateinit var tagPeopleLauncher: ActivityResultLauncher<Intent>
    private lateinit var collaboratorLauncher: ActivityResultLauncher<Intent>
    private lateinit var feelingLauncher: ActivityResultLauncher<Intent>

    // Other imports and class definition



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreatePostBinding.inflate(layoutInflater)
        setContentView(binding.root)
        progressBar = CustomProgressBar(this)
        giffProgressBar = GiffProgressBar(this)
        successGiff = SuccessGiff(this)


        registerReceiver()

        initUI()
        binding.hotelDetailsCv.visibility = View.GONE
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_TAG_PEOPLE && resultCode == Activity.RESULT_OK) {
            val bundle = data?.extras
            val selectedPeopleList = bundle?.getSerializable("selectedPeopleList") as? ArrayList<TagPeople>
            // Handle the selected people list here
        }
    }

    private fun initUI() {

        createServiceNotificationChannel()


        val individualRepo = IndividualRepo(activity)
        individualViewModal = ViewModelProvider(activity, ViewModelFactory(null,individualRepo,null))[IndividualViewModal::class.java]

        preferenceManager = PreferenceManager.getInstance(this)
        val userFullName = preferenceManager.getString(PreferenceManager.Keys.USER_FULL_NAME, "").toString()
        val profilePic = preferenceManager.getString(PreferenceManager.Keys.USER_MEDIUM_PROFILE_PIC, "").toString()
        businessesType = preferenceManager.getString(PreferenceManager.Keys.BUSINESS_TYPE,"").toString()

        Glide.with(this).load(profilePic).placeholder(R.drawable.ic_profile_placeholder).into(binding.userPicIv)
        binding.fullNameTv.text = userFullName

        checkInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            progressBar.hide()
            if (result.resultCode == RESULT_OK) {
                // Get the data from the intent
                val placeAddress = result.data?.getStringExtra("PLACE_ADDRESS") ?: ""
                val placeName = result.data?.getStringExtra("PLACE_NAME") ?: ""
                val placeID = result.data?.getStringExtra("PLACE_ID") ?: ""
                val street = result.data?.getStringExtra("STREET") ?: ""
                val city = result.data?.getStringExtra("CITY") ?: ""
                val state = result.data?.getStringExtra("STATE") ?: ""
                val zipcode = result.data?.getStringExtra("ZIPCODE") ?: ""
                val country = result.data?.getStringExtra("COUNTRY") ?: ""

                val lat = result.data?.getDoubleExtra("LAT", 0.0)
                val lng = result.data?.getDoubleExtra("LNG", 0.0)
                val coverImage = result.data?.getStringExtra("COVER_IMAGE") ?: ""
                val profileImage = result.data?.getStringExtra("PROFILE_PIC") ?: ""
                if (placeAddress.isNotBlank()) {
                    binding.hotelDetailsCv.visibility = View.VISIBLE
                    binding.hotelNameTv.text = placeName
                    binding.hotelAddressTv.text = placeAddress

                    Glide.with(this).load(profileImage).placeholder(R.drawable.ic_profile_placeholder).into(binding.hotelProfileIv)
                    Glide.with(this).load(coverImage).placeholder(R.drawable.ic_image_placeholder_image).into(binding.imageView)

                    selectedPlaceName = "$placeName, $state"
                    selectedLat = lat!!
                    selectedLng = lng!!

                }else{
                    binding.hotelDetailsCv.visibility = View.GONE
                }

            }
        }

        this.blurTheView(binding.blurView)
        setMediaListAdapter()
        binding.userTagLayout.visibility = View.GONE
        // Initialize the ActivityResultLauncher
        tagPeopleLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            handleSelectedTagPeople(result)
        }
        collaboratorLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            handleSelectedCollaborators(result)
        }
        feelingLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            handleSelectedFeeling(result)
        }

        binding.backBtn.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.checkInLayout.setOnClickListener {
                progressBar.show()
                val intent = Intent(this, CheckInActivity::class.java)
                checkInLauncher.launch(intent) // Launch CheckInActivity
        }



        binding.tagPeopleLayout.setOnClickListener {
            val intent = Intent(this, TagPeopleActivity::class.java)
            intent.putExtra("selectedTagPeopleList", selectedTagPeopleList)
            tagPeopleLauncher.launch(intent)
        }

        binding.collaboratorLayout.setOnClickListener {
            val intent = Intent(this, TagPeopleActivity::class.java)
            intent.putExtra("selectedTagPeopleList", selectedCollaborators)
            intent.putExtra("isCollaboration", true)
            collaboratorLauncher.launch(intent)
        }

        binding.feelingLayout.setOnClickListener {
            val intent = Intent(this, FeelingActivity::class.java)
            intent.putExtra("selectedFeeling", selectedFeeling)
            feelingLauncher.launch(intent)
        }

        binding.photoVideoLayout.setOnClickListener {
            if (mediaList.size < enabledMediaCount){
                checkGalleryPermissions()
            }else{
                CustomSnackBar.showSnackBar(binding.root, getString(R.string.max_media_selection_error, enabledMediaCount))
            }
        }

        binding.cameraLayout.setOnClickListener {
            if (mediaList.size < enabledMediaCount){
                checkCameraPermissions()
            }else{
                CustomSnackBar.showSnackBar(binding.root, getString(R.string.max_media_selection_error, enabledMediaCount))
            }
//            checkCameraPermissions()
        }

        binding.doneBtn.setOnClickListener {
//            // Create separate lists for images and videos
//            val imageList = mutableListOf<String>()
//            val videoList = mutableListOf<String>()
//            // Iterate through the mediaList and separate images and videos
//            for (media in mediaList) {
//                when {
//                    media.endsWith(".jpg", true) || media.endsWith(".jpeg", true) || media.endsWith(".png", true) -> {
//                        // Add to image list
//                        imageList.add(media)
//                    }
//                    media.endsWith(".mp4", true) || media.endsWith(".mkv", true) || media.endsWith(".mov", true) -> {
//                        // Add to video list
//                        videoList.add(media)
//                    }
//                    // Add more conditions if needed
//                }
//            }
            val selectedTagIdList = selectedTagPeopleList.map { it.id }.toMutableList()
            val collaboratorIds = selectedCollaborators.map { it.id }
            
            Log.d("CreatePostActivity", "=== POST CREATION DEBUG ===")
            Log.d("CreatePostActivity", "selectedCollaborators size: ${selectedCollaborators.size}")
            Log.d("CreatePostActivity", "selectedCollaborators details: ${selectedCollaborators.map { "${it.name} (id: ${it.id})" }}")
            Log.d("CreatePostActivity", "collaboratorIds extracted: ${collaboratorIds.size} items")
            Log.d("CreatePostActivity", "collaboratorIds values: $collaboratorIds")
            Log.d("CreatePostActivity", "========================")

            val content = binding.contentEt.text.toString().trim()
            if(mediaList.isNotEmpty()){
                createPost(mediaList,selectedTagIdList, collaboratorIds)
            }else{
                if (content.isNotEmpty()){
                    createPost(mediaList,selectedTagIdList, collaboratorIds)
                }else{
                    CustomSnackBar.showSnackBar(binding.root,MessageStore.pleaseAddMedia(this))
                }
            }

        }



        individualViewModal.createNewPostResult.observe(activity){result->
            if (result.status == true){
                val msg = result.message.toString()


                successGiff.show(msg) {
                    // Callback when animation is complete
                    navigateToMainActivity(businessesType == business_type_individual)
                    // Navigate to another activity or perform any action here
                }


            }else{
                val msg = result.message.toString()
                CustomSnackBar.showSnackBar(binding.root,msg)
            }
        }


        individualViewModal.loading.observe(activity){
            if (it == true){
                giffProgressBar.show() // To show the giff progress bar
            }else{
                giffProgressBar.hide() // To hide the giff progress bar
            }
        }

        individualViewModal.toast.observe(activity){
//            Toast.makeText(activity,it, Toast.LENGTH_SHORT).show()
            CustomSnackBar.showSnackBar(binding.root,it)
        }




    }

    private fun handleSelectedCollaborators(result: ActivityResult) {
        if (result.resultCode == Activity.RESULT_OK) {
            val bundle = result.data?.extras
            val selectedPeopleList = bundle?.getSerializable("selectedPeopleList") as? ArrayList<TagPeople>
            if (selectedPeopleList != null) {
                selectedCollaborators = selectedPeopleList
                Log.d("CreatePostActivity", "Selected ${selectedCollaborators.size} collaborators: ${selectedCollaborators.map { "${it.name} (${it.id})" }}")
            } else {
                Log.e("CreatePostActivity", "handleSelectedCollaborators: selectedPeopleList is null")
            }
        } else {
            Log.e("CreatePostActivity", "handleSelectedCollaborators: resultCode is not RESULT_OK, code: ${result.resultCode}")
        }
    }



    private fun getDataFromContentUri(uri: Uri, selection: String?, selectionArgs: Array<String>?): String? {
        var path: String? = null
        val cursor = contentResolver.query(uri, null, selection, selectionArgs, null)

        cursor?.use {
            val columnNames = it.columnNames.joinToString(", ") // Get all available column names
            Log.d("CursorColumns", "Available columns: $columnNames") // Log available columns

            if (it.moveToFirst()) {
                // Check if the _data column exists in the cursor
                try {
                    val columnIndex = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                    path = it.getString(columnIndex)
                } catch (e: IllegalArgumentException) {
                    Log.e("DataRetrievalError", "Column '_data' does not exist")
                    // Handle case where _data is not available
                }
            }
        }
        return path
    }

    private fun handleSelectedTagPeople(result: ActivityResult) {
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            val bundle = data?.extras
//            val selectedPeopleList = bundle?.getSerializable("selectedPeopleList") as? ArrayList<TaggedData>
            val selectedPeopleList = bundle?.getSerializable("selectedPeopleList") as? ArrayList<TagPeople>
            selectedTagPeopleList = selectedPeopleList!!
            setTagsFlexList(selectedPeopleList)
            // Handle the selected people list here
        }
    }


    private fun handleSelectedFeeling(result: ActivityResult) {
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            val bundle = data?.extras
            val selectedFeelings = bundle?.getSerializable("selectedFeeling") as? String
            selectedFeeling = selectedFeelings!!
            binding.feelingTv.text = selectedFeeling
            if (selectedFeeling.isNotEmpty()){
             binding.feelingTv.visibility = View.VISIBLE
            }else{
                binding.feelingTv.visibility = View.GONE
            }
            // Handle the selected people list here
        }
    }

    private fun checkGalleryPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13 and above
                showPhotoVideoDialog()
        } else {
            // Lower versions
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                showPhotoVideoDialog()
            } else {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), PERMISSION_REQUEST_CODE_READ_EXTERNAL_STORAGE)
            }
        }
    }

    private fun checkCameraPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_CODE
            )
        } else {
            captureImageFromCamera()
        }
    }

    private fun captureImageFromCamera() {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraLauncher.launch(cameraIntent)
    }

    private fun createImageFile(): File? {
        return File.createTempFile("IMG_${System.currentTimeMillis()}", ".jpg", cacheDir)
    }

    private fun setMediaListAdapter() {
        if (mediaList.isEmpty()){
            binding.mediaRv.visibility = View.GONE
        }else{
            binding.mediaRv.visibility = View.VISIBLE
            attachedMediaAdapter = AttachedMediaAdapter(this, mediaList, ::onMediaUpdated, null)
            binding.mediaRv.adapter = attachedMediaAdapter
        }

    }

    private fun setTagsFlexList(selectedPeopleList: ArrayList<TagPeople>) {
        binding.userTagLayout.visibility = if (selectedPeopleList.isEmpty()) View.GONE else View.VISIBLE
        val tagAdapter = TagsAdapter(this, selectedPeopleList, ::onTagUpdated)
        binding.tagsRv.adapter = tagAdapter

        val layoutManager = FlexboxLayoutManager(this).apply {
            flexDirection = FlexDirection.ROW
            justifyContent = JustifyContent.FLEX_START
        }
        binding.tagsRv.layoutManager = layoutManager
    }

    private fun onTagUpdated(updatedList: ArrayList<TagPeople>) {
        selectedTagPeopleList = updatedList
        binding.userTagLayout.visibility = if (updatedList.isEmpty()) View.GONE else View.VISIBLE

    }

    private fun onMediaUpdated(updatedMediaList: List<String>) {
        binding.mediaRv.visibility = if (updatedMediaList.isEmpty()) View.GONE else View.VISIBLE
    }

//    private fun showPhotoVideoDialog() {
//        val options = arrayOf("Photos", "Videos")
//
//        val alertDialog = AlertDialog.Builder(this)
//            .setTitle("Select an Option")
//            .setItems(options) { dialog, which ->
//                when (which) {
//                    0 -> pickMultiplePhotos()
//                    1 -> pickSingleVideo()
//                }
//            }
//            .create()
//
//        alertDialog.show()
//    }

    private fun showPhotoVideoDialog() {
        // Create an instance of PhotoVideoDialog
        val dialog = PhotoVideoDialog(
            activity = this,
            title = "Upload",
            photoText = "Photo",
            videoText = "Video",
            photoIconResId = R.drawable.ic_photo_blue,
            videoIconResId = R.drawable.ic_video_blue,
            photoClickListener = { pickMultiplePhotos() },
            videoClickListener = { pickSingleVideo() },
            onDismissListener = {
                // Handle dialog dismiss event
                println("Dialog dismissed")
            },
            autoCancel = true
        )
        // Show the dialog
        dialog.show()
    }

    private fun pickMultiplePhotos() {
//        pickPhotosLauncher.launch(arrayOf("image/*"))
        pickPhotosLauncher.launch("image/*")
    }

    private fun pickSingleVideo() {
        pickVideoLauncher.launch("video/*")
    }

    // Handle permissions results if needed
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSION_REQUEST_CODE_READ_EXTERNAL_STORAGE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    showPhotoVideoDialog()
                }
            }
            PERMISSION_REQUEST_CODE_CAMERA -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    captureImageFromCamera()
                }
            }
        }
    }
    private fun checkVideoDurationAndTrim(uri: Uri) {
        val projection = arrayOf(MediaStore.Video.Media.DURATION)
        val cursor = contentResolver.query(uri, projection, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val durationMillis = it.getLong(it.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION))
                val durationSeconds = durationMillis / 1000

                val savedVideoUri = saveVideo(uri)

                // Launch the video trimmer with the saved video URI
                if (savedVideoUri != null) {
                    showVideoTrimmer(savedVideoUri)
                }
//                showVideoTrimmer(uri)
//                mediaList.add(uri.toString())
//                setMediaListAdapter()
//                if (durationSeconds > 30) {
//                    // Call the video trimmer if video duration exceeds 30 seconds
//                    showVideoTrimmer(uri)
//                } else {
//                    // Add video directly if duration is within the limit
//                    mediaList.add(uri.toString())
//                    setMediaListAdapter()
//                }
            }
        } ?: run {
            Log.e("CreatePostActivity", "Failed to retrieve video duration")
        }
    }

    private fun saveVideo(uri: Uri): Uri? {
        val contentResolver = applicationContext.contentResolver
        val inputStream = contentResolver.openInputStream(uri)

        // Create a unique file name using the current time in milliseconds
        val outputDir = getExternalFilesDir(Environment.DIRECTORY_MOVIES)
        val outputFileName = "video_${System.currentTimeMillis()}.mp4" // Unique filename
        val outputFile = File(outputDir, outputFileName)

        return try {
            val outputStream = FileOutputStream(outputFile)
            inputStream.use { input ->
                outputStream.use { output ->
                    input?.copyTo(output) // Copy the video data
                }
            }
            Uri.fromFile(outputFile) // Return the URI of the saved video
        } catch (e: IOException) {
            Log.e("CreatePostActivity", "Failed to save video: ${e.message}")
            null
        }
    }

    private fun showVideoTrimmer(uri: Uri) {
        Log.d("Video URI before sending", uri.toString()) // Log the URI
        val intent = Intent(this, VideoTrimmerActivity::class.java).apply {
            putExtra("video_uri", uri.toString())
            putExtra("FROM", "CreatePost")
        }
        videoTrimmerLauncher.launch(intent)
    }

    private fun saveImageToStorage(bitmap: Bitmap): Uri? {
        val imagesDir = File(this.applicationContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "HotelMediaImages")
        if (!imagesDir.exists()) {
            imagesDir.mkdirs()
        }
        val imageFile = File(imagesDir, "profile_image_${System.currentTimeMillis()}.jpg")
        try {
            FileOutputStream(imageFile).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
            }
            return Uri.fromFile(imageFile)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    companion object {
        const val PERMISSION_REQUEST_CODE_READ_EXTERNAL_STORAGE = 1001
        private const val PERMISSION_REQUEST_CODE_CAMERA = 1002
        const val PERMISSION_REQUEST_CODE_WRITE_EXTERNAL_STORAGE = 102
    }


    private fun createPost(mediaList: MutableList<String>, selectedTagIdList: MutableList<String>, collaboratorIds: List<String>) {
        val content = binding.contentEt.text.toString().trim()
        successGiff.show(getString(R.string.post_uploaded_will_be_live_shortly)) {
            // Callback when animation is complete
            startCreatePostWorker(this, mediaList, selectedTagIdList, content, selectedPlaceName, selectedLat, selectedLng, selectedFeeling, collaboratorIds)
        }
    }



    override fun onResume() {
        super.onResume()
        registerReceiver()
    }

    private fun registerReceiver() {
        val filter = IntentFilter(Constants.CREATE_POST_BROADCAST)
        LocalBroadcastManager.getInstance(this).registerReceiver(serviceReceiver, filter)
    }


    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(serviceReceiver)
    }



    private fun createServiceNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val channelId = "create_post_channel"
                val channelName = "Create Post Notifications"
                val importance = NotificationManager.IMPORTANCE_HIGH

                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                // Check if the channel already exists to avoid recreating it
                val existingChannel = notificationManager.getNotificationChannel(channelId)
                if (existingChannel == null) {
                    val channel = NotificationChannel(channelId, channelName, importance)
                    notificationManager.createNotificationChannel(channel)
                }
            } catch (e: Exception) {
                Log.e("NotificationChannel", "Failed to create notification channel", e)
            }
        }
    }

}
