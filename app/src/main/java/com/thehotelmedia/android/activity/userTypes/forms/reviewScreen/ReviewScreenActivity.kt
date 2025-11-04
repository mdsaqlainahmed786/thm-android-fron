package com.thehotelmedia.android.activity.userTypes.forms.reviewScreen

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.thehotelmedia.android.BuildConfig
import com.thehotelmedia.android.R
import com.thehotelmedia.android.ViewModelFactory
import com.thehotelmedia.android.activity.BaseActivity
import com.thehotelmedia.android.activity.userTypes.forms.CheckInActivity
import com.thehotelmedia.android.activity.userTypes.forms.CreatePostActivity.Companion.PERMISSION_REQUEST_CODE_READ_EXTERNAL_STORAGE
import com.thehotelmedia.android.activity.userTypes.forms.EditImageActivity
import com.thehotelmedia.android.activity.userTypes.forms.VideoTrimmerActivity
import com.thehotelmedia.android.adapters.userTypes.individual.forms.AttachedMediaAdapter
import com.thehotelmedia.android.adapters.userTypes.individual.forms.review.ReviewRatingAdapter
import com.thehotelmedia.android.customClasses.Constants.business_type_individual
import com.thehotelmedia.android.customClasses.CustomProgressBar
import com.thehotelmedia.android.customClasses.CustomSnackBar
import com.thehotelmedia.android.customClasses.GiffProgressBar
import com.thehotelmedia.android.customClasses.MessageStore
import com.thehotelmedia.android.customClasses.PreferenceManager
import com.thehotelmedia.android.customClasses.SuccessGiff
import com.thehotelmedia.android.customDialog.PhotoVideoDialog
import com.thehotelmedia.android.databinding.ActivityReviewScreenBinding
import com.thehotelmedia.android.extensions.EncryptionHelper
import com.thehotelmedia.android.extensions.blurTheView
import com.thehotelmedia.android.extensions.capitalizeFirstLetter
import com.thehotelmedia.android.extensions.navigateToMainActivity
import com.thehotelmedia.android.extensions.toggleEnable
import com.thehotelmedia.android.modals.checkinData.checkInData.ReviewQuestions
import com.thehotelmedia.android.repository.IndividualRepo
import com.thehotelmedia.android.viewModal.individualViewModal.IndividualViewModal
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class ReviewScreenActivity : BaseActivity() {

    private lateinit var binding: ActivityReviewScreenBinding
    private lateinit var checkInLauncher: ActivityResultLauncher<Intent>
    private lateinit var progressBar: CustomProgressBar
    private val selectedRating = mutableMapOf<String, Int>()
    private val staticRating = mutableMapOf<String, Int>()
    private val activity = this@ReviewScreenActivity
    private lateinit var individualViewModal: IndividualViewModal
    private lateinit var preferenceManager : PreferenceManager
    private lateinit var successGiff: SuccessGiff
    private lateinit var giffProgressBar: GiffProgressBar
    private var placeName: String = ""
    private var typeOfId: String = ""
    private var businessId: String = ""
    private var placeID: String = ""
    private var street: String = ""
    private var city: String = ""
    private var state: String = ""
    private var country: String = ""
    private var zipcode: String = ""
    private var lat: Double = 0.0
    private var lng: Double = 0.0


    private lateinit var attachedMediaAdapter: AttachedMediaAdapter

    private val mediaList = mutableListOf<String>()

    private var enabledMediaCount: Int = 6
    private val videoTrimmerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val trimmedVideoUri = result.data?.getStringExtra("trimmed_video_uri")
            trimmedVideoUri?.let {
                mediaList.add(it)
                setMediaListAdapter()
            }
        }
    }

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


    private fun setMediaListAdapter() {
        if (mediaList.isEmpty()){
            binding.mediaRv.visibility = View.GONE
        }else{
            binding.mediaRv.visibility = View.VISIBLE
            attachedMediaAdapter = AttachedMediaAdapter(this, mediaList, ::onMediaUpdated)
            binding.mediaRv.adapter = attachedMediaAdapter
        }

    }

    private fun onMediaUpdated(updatedMediaList: List<String>) {
        binding.mediaRv.visibility = if (updatedMediaList.isEmpty()) View.GONE else View.VISIBLE
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
    private fun showVideoTrimmer(uri: Uri) {
        Log.d("Video URI before sending", uri.toString()) // Log the URI
        val intent = Intent(this, VideoTrimmerActivity::class.java).apply {
            putExtra("video_uri", uri.toString())
            putExtra("FROM", "CreatePost")
        }
        videoTrimmerLauncher.launch(intent)
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


    // Define ActivityResultLauncher for picking a single video
    private val pickVideoLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            // Add the video to the mediaList
            checkVideoDurationAndTrim(uri)
//            mediaList.add(uri.toString())
//            setMediaListAdapter()
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReviewScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)
        progressBar = CustomProgressBar(this)
        giffProgressBar = GiffProgressBar(this)
        successGiff = SuccessGiff(this)
        binding.ratingLayout.visibility = View.GONE
        inItUi()
    }

    private fun inItUi() {
        this.blurTheView(binding.blurView)
        val individualRepo = IndividualRepo(activity)
        individualViewModal = ViewModelProvider(activity, ViewModelFactory(null,individualRepo,null))[IndividualViewModal::class.java]
        preferenceManager = PreferenceManager.getInstance(this)
        val businessesType = preferenceManager.getString(PreferenceManager.Keys.BUSINESS_TYPE,"").toString()
        enableDoneBtn(false)

        if (intent.action == Intent.ACTION_VIEW) {
            val uri: Uri? = intent.data
            println("sdjakjfdska;k   uri  $uri")
            uri?.let {
                println("sdjakjfdska;k   it  $it")
                val id = it.getQueryParameter("id").toString()
                placeID = it.getQueryParameter("placeID").toString()

                // Extract the encrypted values directly
                val encryptedId = it.getQueryParameter("id").toString()
                // Decrypt the values
                val decryptedId = EncryptionHelper.decrypt(encryptedId)


                println("sdafjklasdhk    decryptedId $decryptedId")
                println("sdafjklasdhk    placeId $placeID")
                individualViewModal.getCheckInData(placeID,decryptedId)


                // Now, you have the category ID and name, you can use them to populate the UI or perform any necessary actions
            }
        }else{

            val from = intent.getStringExtra("FROM") ?: ""
            if (from == "UserProfileDetailScreen"){
                checkDataFromScreen()
            }

        }




        // Initialize the launcher to handle results from CheckInActivity
        checkInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            progressBar.hide()
            if (result.resultCode == RESULT_OK) {
                // Get the data from the intent

                val placeAddress = result.data?.getStringExtra("PLACE_ADDRESS") ?: ""
                placeName = result.data?.getStringExtra("PLACE_NAME") ?: ""
                placeID = result.data?.getStringExtra("PLACE_ID") ?: ""
                street = result.data?.getStringExtra("STREET") ?: ""
                city = result.data?.getStringExtra("CITY") ?: ""
                state = result.data?.getStringExtra("STATE") ?: ""
                zipcode = result.data?.getStringExtra("ZIPCODE") ?: ""
                country = result.data?.getStringExtra("COUNTRY") ?: ""
                lat = result.data?.getDoubleExtra("LAT", 0.0)!!
                lng = result.data?.getDoubleExtra("LNG", 0.0)!!
                val coverImage = result.data?.getStringExtra("COVER_IMAGE") ?: ""
                val profileImage = result.data?.getStringExtra("PROFILE_PIC") ?: ""
                businessId = result.data?.getStringExtra("BUSINESS_ID") ?: ""
                typeOfId = result.data?.getStringExtra("TYPE_OF_ID") ?: ""

                val reviewQuestionJson = result.data?.getStringExtra("REVIEW_QUESTION")
                val gson = Gson()
                val reviewQuestionType = object : TypeToken<ArrayList<ReviewQuestions>>() {}.type
                val reviewQuestions: ArrayList<ReviewQuestions>? = gson.fromJson(reviewQuestionJson, reviewQuestionType)


                val reviewRatingAdapter = ReviewRatingAdapter(this,reviewQuestions!!,::onRatingSelected)
                binding.reviewRatingRv.adapter = reviewRatingAdapter

                if (placeAddress.isNotBlank()) {
                    enableDoneBtn(true)
                    binding.ratingLayout.visibility = View.VISIBLE

                    binding.hotelNameTv.text = placeName
                    binding.hotelAddressTv.text = placeAddress

                    Glide.with(this).load(profileImage).placeholder(R.drawable.ic_profile_placeholder).into(binding.hotelProfileIv)
                    Glide.with(this).load(coverImage).placeholder(R.drawable.ic_image_placeholder_image).into(binding.imageView)

                }else{
                    enableDoneBtn(false)
                    binding.ratingLayout.visibility = View.GONE
                }

            }
        }




        binding.photoVideoLayout.setOnClickListener {
            if (mediaList.size < enabledMediaCount){
                checkGalleryPermissions()
            }else{
                CustomSnackBar.showSnackBar(binding.root, getString(R.string.max_media_selection_error, enabledMediaCount))
            }
        }


        binding.doneButton.setOnClickListener {

            val description = binding.descriptionEt.text.toString().trim()
            if (selectedRating.size < (binding.reviewRatingRv.adapter?.itemCount ?: 0)) {
                CustomSnackBar.showSnackBar(binding.root,MessageStore.rateAllQuestions(this))
            }else if (description.isBlank()){
                CustomSnackBar.showSnackBar(binding.root,MessageStore.descriptionBeforeProceeding(this))
            }else{
                val answerJson = getSelectedAnswers()

                println("safjlksadklasjkl   $answerJson")
//                [{ "questionID": "not-indexed", "rating": 4 }]

                sendRatingAnswer(answerJson,description)
            }

        }





        binding.backBtn.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.checkInLayout.setOnClickListener {
            progressBar.show()
            val intent = Intent(this, CheckInActivity::class.java)
            checkInLauncher.launch(intent) // Launch CheckInActivity
        }



        individualViewModal.createReviewResult.observe(activity){result->
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



        individualViewModal.getCheckInDataResult.observe(this){result->
            if (result.status == true){

                val mapApiKey = "&key=${BuildConfig.MAPS_API_KEY}"

                businessId = result.data?.businessProfileRef?.id ?: ""
                typeOfId = result.data?.businessProfileRef?.type ?: ""
                var coverImage = result.data?.businessProfileRef?.coverImage ?: ""
                var profileImage = result.data?.businessProfileRef?.profilePic?.medium ?: ""

                if (businessId.isEmpty()){
                    coverImage = "$coverImage$mapApiKey"
                    profileImage = "$profileImage$mapApiKey"
                }

                val businessName = result.data?.businessProfileRef?.name?.capitalizeFirstLetter() ?: ""
                street = result.data?.businessProfileRef?.address?.street ?: ""
                city = result.data?.businessProfileRef?.address?.city ?: ""
                state = result.data?.businessProfileRef?.address?.state ?: ""
                zipcode = result.data?.businessProfileRef?.address?.zipCode ?: ""
                country = result.data?.businessProfileRef?.address?.country ?: ""
                lat = result.data?.businessProfileRef?.address?.lat ?: 0.0
                lng = result.data?.businessProfileRef?.address?.lng ?: 0.0

                val reviewQuestions = result.data?.reviewQuestions
                if (street.isBlank()){
                    street = businessName
                }
                val placeAddress = "$city, $state, $country, $zipcode"
                placeName = businessName


                val reviewRatingAdapter = ReviewRatingAdapter(this,reviewQuestions!!,::onRatingSelected)
                binding.reviewRatingRv.adapter = reviewRatingAdapter
                if (placeAddress.isNotBlank()) {
                    enableDoneBtn(true)
                    binding.ratingLayout.visibility = View.VISIBLE

                    binding.hotelNameTv.text = placeName
                    binding.hotelAddressTv.text = placeAddress

                    Glide.with(this).load(profileImage).placeholder(R.drawable.ic_profile_placeholder).into(binding.hotelProfileIv)
                    Glide.with(this).load(coverImage).placeholder(R.drawable.ic_image_placeholder_image).into(binding.imageView)

                }else{
                    enableDoneBtn(false)
                    binding.ratingLayout.visibility = View.GONE
                }





            }else{
                val msg = result.message
                Toast.makeText(this,msg, Toast.LENGTH_SHORT).show()
            }
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

    private fun sendRatingAnswer(answerJson: List<Map<String, Any>>, description: String) {

        var review = answerJson
        if (answerJson.isEmpty()){
//            review = """[{"questionID":"not-indexed","rating":3}]"""
            review = printStaticRatingAnswers()
        }
        println("asfjdshjka   $review")

        // Create separate lists for images and videos
        val imageList = mutableListOf<String>()
        val videoList = mutableListOf<String>()


        for (media in mediaList) {
            when {
                media.endsWith(".jpg", true) || media.endsWith(".jpeg", true) || media.endsWith(".png", true) -> {
                    // Add to image list
                    imageList.add(media)
                }
                media.endsWith(".mp4", true) || media.endsWith(".mkv", true) || media.endsWith(".mov", true) -> {
                    // Add to video list
                    videoList.add(media)
                }
                // Add more conditions if needed
            }
        }



        var anonymousUserID = ""
        if (typeOfId == "google-business-profile"){
            anonymousUserID = businessId
            businessId = ""
        }else{
            anonymousUserID = ""
        }
        individualViewModal.createReview(businessId,description,placeID,review,placeName,street,city,state,zipcode,country,lat,lng,imageList,videoList,anonymousUserID)

    }

//    private fun printSelectedAnswers(): String? {
//        val gson = Gson()
//        val json = gson.toJson(selectedRating.map { (question, rating) ->
//            mapOf("questionID" to question, "rating" to rating)
//        })
//        return json
//    }
    private fun getSelectedAnswers(): List<Map<String, Any>> {
        return selectedRating.map { (question, rating) ->
            mapOf("questionID" to question, "rating" to rating)
        }
    }


    private fun printStaticRatingAnswers(): List<Map<String, Any>> {
        // Assign a value to the map before using it
        staticRating["not-indexed"] = 5

        // Map the staticRating entries to a list of maps
        return staticRating.map { (question, rating) ->
            mapOf("questionID" to question, "rating" to rating)
        }
    }

//    private fun printStaticRatingAnswers(): String? {
//        staticRating["not-indexed"] = 5
//        val gson = Gson()
//        val json = gson.toJson(staticRating.map { (question, rating) ->
//            mapOf("questionID" to question, "rating" to rating)
//        })
//        return json
//    }



//private fun printSelectedAnswers(): JsonArray? {
//    val gson = Gson()
//    val json = gson.toJson(selectedRating.map { (question, rating) ->
//        mapOf("questionID" to question, "rating" to rating)
//    })
//
//    // Convert the JSON string to a JsonArray
//    return JsonParser.parseString(json).asJsonArray
//}

    private fun enableDoneBtn(status: Boolean) {
        if (status == false){
            binding.doneButton.toggleEnable(false)
        }else{
            binding.doneButton.toggleEnable(true)
        }
    }
    private fun onRatingSelected(questionId: String, rating: Int) {
        selectedRating[questionId] = rating
    }


    private fun checkDataFromScreen() {
        val placeAddress = intent.getStringExtra("PLACE_ADDRESS") ?: ""
        placeName = intent.getStringExtra("PLACE_NAME") ?: ""
        placeID = intent.getStringExtra("PLACE_ID") ?: ""
        street = intent.getStringExtra("STREET") ?: ""
        city = intent.getStringExtra("CITY") ?: ""
        state = intent.getStringExtra("STATE") ?: ""
        zipcode = intent.getStringExtra("ZIPCODE") ?: ""
        country = intent.getStringExtra("COUNTRY") ?: ""
        lat = intent.getDoubleExtra("LAT", 0.0)
        lng = intent.getDoubleExtra("LNG", 0.0)
        val coverImage = intent.getStringExtra("COVER_IMAGE") ?: ""
        val profileImage = intent.getStringExtra("PROFILE_PIC") ?: ""
        businessId = intent.getStringExtra("BUSINESS_ID") ?: ""
        typeOfId = intent.getStringExtra("TYPE_OF_ID") ?: ""
        val reviewQuestionJson = intent.getStringExtra("REVIEW_QUESTION")
        val gson = Gson()
        val reviewQuestionType = object : TypeToken<ArrayList<ReviewQuestions>>() {}.type
        val reviewQuestions: ArrayList<ReviewQuestions>? = gson.fromJson(reviewQuestionJson, reviewQuestionType)


        val reviewRatingAdapter = ReviewRatingAdapter(this,reviewQuestions!!,::onRatingSelected)
        binding.reviewRatingRv.adapter = reviewRatingAdapter
        if (placeAddress.isNotBlank()) {
            enableDoneBtn(true)
            binding.ratingLayout.visibility = View.VISIBLE

            binding.hotelNameTv.text = placeName
            binding.hotelAddressTv.text = placeAddress

            Glide.with(this).load(profileImage).placeholder(R.drawable.ic_profile_placeholder).into(binding.hotelProfileIv)
            Glide.with(this).load(coverImage).placeholder(R.drawable.ic_image_placeholder_image).into(binding.imageView)

        }else{
            enableDoneBtn(false)
            binding.ratingLayout.visibility = View.GONE
        }
    }


}