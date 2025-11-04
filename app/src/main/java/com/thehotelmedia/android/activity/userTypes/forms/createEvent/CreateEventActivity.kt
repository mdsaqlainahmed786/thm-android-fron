package com.thehotelmedia.android.activity.userTypes.forms.createEvent

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.AutocompleteActivity
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.thehotelmedia.android.BuildConfig
import com.thehotelmedia.android.R
import com.thehotelmedia.android.ViewModelFactory
import com.thehotelmedia.android.activity.BaseActivity
import com.thehotelmedia.android.customClasses.Constants.business_type_individual
import com.thehotelmedia.android.customClasses.CustomProgressBar
import com.thehotelmedia.android.customClasses.CustomSnackBar
import com.thehotelmedia.android.customClasses.GiffProgressBar
import com.thehotelmedia.android.customClasses.MessageStore
import com.thehotelmedia.android.customClasses.PreferenceManager
import com.thehotelmedia.android.customClasses.SuccessGiff
import com.thehotelmedia.android.databinding.ActivityCreateEventBinding
import com.thehotelmedia.android.extensions.hideKeyboard
import com.thehotelmedia.android.extensions.navigateToMainActivity
import com.thehotelmedia.android.repository.IndividualRepo
import com.thehotelmedia.android.viewModal.individualViewModal.IndividualViewModal
import com.yalantis.ucrop.UCrop
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale


class CreateEventActivity : BaseActivity() {

    private lateinit var binding : ActivityCreateEventBinding
    private val activity = this@CreateEventActivity
    private var eventFormat = ""
    private var startDate = ""
    private var startTime = ""
    private var endDate = ""
    private var endTime = ""
    private var coverImage = ""
    private var businessesType = ""
    private lateinit var preferenceManager : PreferenceManager
    private lateinit var progressBar : CustomProgressBar
    private lateinit var giffProgressBar : GiffProgressBar
    private lateinit var successGiff: SuccessGiff
    private lateinit var individualViewModal: IndividualViewModal

    private val tag: String = "CREATE_EVENT_SCREEN"
    private val galleryLauncher: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                val savedUri = saveImageToStorageFromUri(it)
                savedUri?.let {
                    openCropImageScreen(it)
                }
            }
        }
    private fun saveImageToStorageFromUri(uri: Uri): Uri? {
        val imagesDir =
            File(this.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "HotelMediaImages")
        if (!imagesDir.exists()) {
            imagesDir.mkdirs()
        }
        val imageFile = File(imagesDir, "profile_image.jpg")
        try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(imageFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            return Uri.fromFile(imageFile)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
    private fun openCropImageScreen(uri: Uri) {
        UCrop.of(
            uri,
            Uri.fromFile(File(cacheDir, "cropped_image_${System.currentTimeMillis()}.jpg"))
        )
            .withAspectRatio(3f, 4f)
            .start(this, cropActivityResultLauncher)
    }

    private val cropActivityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                val resultUri = UCrop.getOutput(data!!)
                if (resultUri != null) {
                        binding.coverImage.setImageURI(resultUri)
                        coverImage = resultUri.path.toString()

                }
            }
        }
    private var apiKey: String? = null
    private val AUTOCOMPLETE_REQUEST_CODE = 1
    private var venueAddress = ""
    private var venueName = ""
    private var placeID = ""
    private var eventLatitude = 0.0
    private var eventLongitude = 0.0
    private val autocompleteLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val requestCode = AUTOCOMPLETE_REQUEST_CODE
        val resultCode = result.resultCode
        val data = result.data
        if (requestCode == AUTOCOMPLETE_REQUEST_CODE) {
            when (resultCode) {
                Activity.RESULT_OK -> {
                    val place = Autocomplete.getPlaceFromIntent(data)
//                    Log.d(tag, "Place: ${place.name}, ${place.id}, ${place.address}, latLng ${place.latLng}, places $place")

                    placeID = place.id?.toString() ?: ""
                    venueName = place.name?.toString() ?: ""
                    venueAddress = place.address?.toString() ?: ""
                    eventLatitude = place.latLng?.latitude ?: 0.0
                    eventLongitude = place.latLng?.longitude ?: 0.0
                    binding.venueEt.setText(venueAddress)
//                    binding.venueEt.setText(venue)
//                    binding.venueTv.text = venue

                }
                AutocompleteActivity.RESULT_ERROR -> {
                    val status = Autocomplete.getStatusFromIntent(data)
                    Toast.makeText(this, "${status.statusMessage}", Toast.LENGTH_LONG).show()
                    Log.d(tag, status.statusMessage ?: "Unknown error")
                }
                Activity.RESULT_CANCELED -> {
                    Log.wtf("Error", "Operation canceled")
                }
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateEventBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initPlaces()
        initUi()
    }
    private fun initPlaces() {
        apiKey = BuildConfig.MAPS_API_KEY
        if (!Places.isInitialized()) {
            apiKey?.let { Places.initialize(applicationContext, it) }
        }
    }

    private fun initUi() {
        preferenceManager = PreferenceManager.getInstance(activity)
        val individualRepo = IndividualRepo(activity)
        individualViewModal = ViewModelProvider(activity, ViewModelFactory(null,individualRepo,null))[IndividualViewModal::class.java]
        businessesType = preferenceManager.getString(PreferenceManager.Keys.BUSINESS_TYPE,"").toString()
        giffProgressBar = GiffProgressBar(activity) // 'this' refers to the context
        successGiff = SuccessGiff(activity) // 'this' refers to the context
        progressBar = CustomProgressBar(activity) // 'this' refers to the context

        binding.streamLinkTv.visibility = View.GONE
        binding.streamLinkEt.visibility = View.GONE
        binding.optionalTv.visibility = View.GONE
        binding.venueTv.visibility = View.GONE
        binding.venueEt.visibility = View.GONE

        val userName = preferenceManager.getString(PreferenceManager.Keys.USER_FULL_NAME,"")
        val street = preferenceManager.getString(PreferenceManager.Keys.USER_STREET,"")
        val city = preferenceManager.getString(PreferenceManager.Keys.USER_CITY,"")
        val state = preferenceManager.getString(PreferenceManager.Keys.USER_STATE,"")
        val country = preferenceManager.getString(PreferenceManager.Keys.USER_COUNTRY,"")
        val zipCode = preferenceManager.getString(PreferenceManager.Keys.USER_ZIPCODE,"")
        val profilePic = preferenceManager.getString(PreferenceManager.Keys.USER_MEDIUM_PROFILE_PIC,"")
        Glide.with(activity).load(profilePic).placeholder(R.drawable.ic_profile_placeholder).into(binding.profileIv)


        binding.fullNameTv.text = userName
        binding.addressTv.text = "$street, $city, $state, $country, $zipCode"

//        binding.doneBtn.toggleEnable(false)

        binding.coverImage.setOnClickListener {
            openGallery()
        }

        binding.backBtn.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }


        binding.venueEt.setOnClickListener {
            try{
                val fields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG)
                val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, fields).build(this)
                autocompleteLauncher.launch(intent)
            }catch (e: Exception){
                Toast.makeText(this, e.toString(), Toast.LENGTH_SHORT).show()
            }

        }

        binding.onlineBtn.setOnClickListener {
            eventFormat = "online"
            binding.onlineBtn.setBackgroundResource(R.drawable.rounded_edit_text_background_focused)
            binding.offlineBtn.setBackgroundResource(R.drawable.rounded_edit_text_background_normal)

            binding.streamLinkTv.visibility = View.VISIBLE
            binding.streamLinkEt.visibility = View.VISIBLE
            binding.optionalTv.visibility = View.GONE
            binding.venueTv.visibility = View.GONE
            binding.venueEt.visibility = View.GONE
        }

        binding.offlineBtn.setOnClickListener {
            eventFormat = "offline"
            binding.offlineBtn.setBackgroundResource(R.drawable.rounded_edit_text_background_focused)
            binding.onlineBtn.setBackgroundResource(R.drawable.rounded_edit_text_background_normal)

            binding.streamLinkTv.visibility = View.VISIBLE
            binding.streamLinkEt.visibility = View.VISIBLE
            binding.optionalTv.visibility = View.VISIBLE
            binding.venueTv.visibility = View.VISIBLE
            binding.venueEt.visibility = View.VISIBLE
        }

//        binding.startDateBtn.setOnClickListener { // Get the current date as default
//            val calendar: Calendar = Calendar.getInstance()
//            // Create a DatePickerDialog with the current date
//            val datePickerDialog = DatePickerDialog(
//                ContextThemeWrapper(this, R.style.CustomTimePickerDialog), // Apply the custom theme
//                { _, year, monthOfYear, dayOfMonth -> // Format the selected date
//                    val selectedDate: Calendar = Calendar.getInstance()
//                    selectedDate.set(year, monthOfYear, dayOfMonth)
//                    // Format for "dd MMM, yyyy"
//                    val dateFormat1: SimpleDateFormat = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault())
//                    val formattedDate1: String = dateFormat1.format(selectedDate.time)
//
//                    // Format for "yyyy-MM-dd"
//                    val dateFormat2: SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
//                    val formattedDate2: String = dateFormat2.format(selectedDate.time)
//
//                    // Set the formatted date to the TextView
//                    startDate = formattedDate2 // You can keep this if you need the formatted date elsewhere
//                    binding.startDateBtn.text = formattedDate1 // This is what will be displayed on the button
//                },
//                calendar.get(Calendar.YEAR),
//                calendar.get(Calendar.MONTH),
//                calendar.get(Calendar.DAY_OF_MONTH)
//            )
//            // Restrict the DatePicker to allow only future dates
//            datePickerDialog.datePicker.minDate = System.currentTimeMillis()
//            datePickerDialog.setOnShowListener {
//                val positiveButton = datePickerDialog.getButton(TimePickerDialog.BUTTON_POSITIVE)
//                val negativeButton = datePickerDialog.getButton(TimePickerDialog.BUTTON_NEGATIVE)
//                // Set color of positive and negative buttons
//                positiveButton.setTextColor(ContextCompat.getColor(this, R.color.blue))
//                negativeButton.setTextColor(ContextCompat.getColor(this, R.color.blue))
//            }
//            // Show the DatePickerDialog
//            datePickerDialog.show()
//        }

//        binding.endDateBtn.setOnClickListener { // Get the current date as default
//            val calendar: Calendar = Calendar.getInstance()
//            // Create a DatePickerDialog with the current date
//            val datePickerDialog = DatePickerDialog(
//                ContextThemeWrapper(this, R.style.CustomTimePickerDialog), // Apply the custom theme
//                { _, year, monthOfYear, dayOfMonth -> // Format the selected date
//                    val selectedDate: Calendar = Calendar.getInstance()
//                    selectedDate.set(year, monthOfYear, dayOfMonth)
//                    // Format for "dd MMM, yyyy"
//                    val dateFormat1: SimpleDateFormat = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault())
//                    val formattedDate1: String = dateFormat1.format(selectedDate.time)
//                    // Format for "yyyy-MM-dd"
//                    val dateFormat2: SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
//                    val formattedDate2: String = dateFormat2.format(selectedDate.time)
//                    // Set the formatted date to the TextView
//                    endDate = formattedDate2 // You can keep this if you need the formatted date elsewhere
//                    binding.endDateBtn.text = formattedDate1 // This is what will be displayed on the button
//                },
//                calendar.get(Calendar.YEAR),
//                calendar.get(Calendar.MONTH),
//                calendar.get(Calendar.DAY_OF_MONTH)
//            )
//            // Restrict the DatePicker to allow only future dates
//            datePickerDialog.datePicker.minDate = System.currentTimeMillis()
//            datePickerDialog.setOnShowListener {
//                val positiveButton = datePickerDialog.getButton(TimePickerDialog.BUTTON_POSITIVE)
//                val negativeButton = datePickerDialog.getButton(TimePickerDialog.BUTTON_NEGATIVE)
//                // Set color of positive and negative buttons
//                positiveButton.setTextColor(ContextCompat.getColor(this, R.color.blue))
//                negativeButton.setTextColor(ContextCompat.getColor(this, R.color.blue))
//            }
//            // Show the DatePickerDialog
//            datePickerDialog.show()
//        }


//        binding.startTimeBtn.setOnClickListener { // Get the current time as default
//            val calendar = Calendar.getInstance()
//            val currentHour = calendar[Calendar.HOUR_OF_DAY]
//            val currentMinute = calendar[Calendar.MINUTE]
//            // Create a TimePickerDialog
//            val timePickerDialog = TimePickerDialog(
//                ContextThemeWrapper(this, R.style.CustomTimePickerDialog),
//                { _, hourOfDay, minute -> // Create a Calendar object to hold the selected time
//                    val selectedTime = Calendar.getInstance()
//                    selectedTime[Calendar.HOUR_OF_DAY] = hourOfDay
//                    selectedTime[Calendar.MINUTE] = minute
//                    // Format the selected time to "hh:mm a" (e.g., 11:00 AM)
//                    val timeFormat12Hour = SimpleDateFormat("hh:mm a", Locale.getDefault())
//                    val formattedTime12Hour = timeFormat12Hour.format(selectedTime.time)
//                    // Format the selected time to "HH:mm" (e.g., 17:00)
//                    val timeFormat24Hour = SimpleDateFormat("HH:mm", Locale.getDefault())
//                    val formattedTime24Hour = timeFormat24Hour.format(selectedTime.time)
//                    // Set the formatted time to the TextView
//                    startTime = formattedTime24Hour // Save the 12-hour formatted time to the variable
//                    binding.startTimeBtn.text = formattedTime12Hour // Set both formats to the button text
//                },
//                currentHour,
//                currentMinute,
//                false // `false` for 12-hour time format
//            )
//            timePickerDialog.setOnShowListener {
//                val positiveButton = timePickerDialog.getButton(TimePickerDialog.BUTTON_POSITIVE)
//                val negativeButton = timePickerDialog.getButton(TimePickerDialog.BUTTON_NEGATIVE)
//                // Set color of positive and negative buttons
//                positiveButton.setTextColor(ContextCompat.getColor(this, R.color.blue))
//                negativeButton.setTextColor(ContextCompat.getColor(this, R.color.blue))
//            }
//            // Show the TimePickerDialog
//            timePickerDialog.show()
//        }

//        binding.endTimeBtn.setOnClickListener { // Get the current time as default
//            val calendar = Calendar.getInstance()
//            val currentHour = calendar[Calendar.HOUR_OF_DAY]
//            val currentMinute = calendar[Calendar.MINUTE]
//            // Create a TimePickerDialog
//            val timePickerDialog = TimePickerDialog(
//                ContextThemeWrapper(this, R.style.CustomTimePickerDialog), // Apply the custom theme
//                { _, hourOfDay, minute -> // Create a Calendar object to hold the selected time
//                    val selectedTime = Calendar.getInstance()
//                    selectedTime[Calendar.HOUR_OF_DAY] = hourOfDay
//                    selectedTime[Calendar.MINUTE] = minute
//                    // Format the selected time to "hh:mm a" (e.g., 11:00 AM)
//                    val timeFormat12Hour = SimpleDateFormat("hh:mm a", Locale.getDefault())
//                    val formattedTime12Hour = timeFormat12Hour.format(selectedTime.time)
//                    // Format the selected time to "HH:mm" (e.g., 17:00)
//                    val timeFormat24Hour = SimpleDateFormat("HH:mm", Locale.getDefault())
//                    val formattedTime24Hour = timeFormat24Hour.format(selectedTime.time)
//                    // Set the formatted time to the TextView
//                    endTime = formattedTime24Hour // Save the 12-hour formatted time to the variable
//                    binding.endTimeBtn.text = formattedTime12Hour // Set both formats to the button text
//                },
//                currentHour,
//                currentMinute,
//                false // `false` for 12-hour time format
//            )
//            timePickerDialog.setOnShowListener {
//                val positiveButton = timePickerDialog.getButton(TimePickerDialog.BUTTON_POSITIVE)
//                val negativeButton = timePickerDialog.getButton(TimePickerDialog.BUTTON_NEGATIVE)
//                // Set color of positive and negative buttons
//                positiveButton.setTextColor(ContextCompat.getColor(this, R.color.blue))
//                negativeButton.setTextColor(ContextCompat.getColor(this, R.color.blue))
//            }
//            // Show the TimePickerDialog
//            timePickerDialog.show()
//        }


        binding.startDateBtn.setOnClickListener {
            showDatePicker(true)    // Pass true to handle start date selection
        }
        binding.endDateBtn.setOnClickListener {
            showDatePicker(false)    // Pass false to handle end date selection
        }

        binding.startTimeBtn.setOnClickListener {
            showTimePicker(true)    // Pass true to handle start time selection
        }
        binding.endTimeBtn.setOnClickListener {
            showTimePicker(false)    // Pass false to handle end time selection
        }

        binding.doneBtn.setOnClickListener {
            validateFields()
        }


        individualViewModal.createEventResult.observe(activity){result->
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


    private fun showTimePicker(isStartTime: Boolean) {
        val calendar = Calendar.getInstance()
        val currentHour = calendar[Calendar.HOUR_OF_DAY]
        val currentMinute = calendar[Calendar.MINUTE]

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val formattedCurrentDate = dateFormat.format(calendar.time)

        // Check if we are dealing with start or end time
        val selectedDate = if (isStartTime) startDate else endDate

        // If selected date is today, restrict past time selection
        val timePickerDialog = TimePickerDialog(
            ContextThemeWrapper(this, R.style.CustomTimePickerDialog),
            { _, hourOfDay, minute ->
                val selectedTime = Calendar.getInstance()
                selectedTime[Calendar.HOUR_OF_DAY] = hourOfDay
                selectedTime[Calendar.MINUTE] = minute

                // Prevent selecting past time if the selected date is today
                if (selectedDate == formattedCurrentDate &&
                    (hourOfDay < currentHour || (hourOfDay == currentHour && minute < currentMinute))
                ) {
                    Toast.makeText(this, getString(R.string.cant_select_past_time), Toast.LENGTH_SHORT).show()
                } else {
                    val timeFormat12Hour = SimpleDateFormat("hh:mm a", Locale.getDefault())
                    val formattedTime12Hour = timeFormat12Hour.format(selectedTime.time)

                    val timeFormat24Hour = SimpleDateFormat("HH:mm", Locale.getDefault())
                    val formattedTime24Hour = timeFormat24Hour.format(selectedTime.time)

                    if (isStartTime) {
                        startTime = formattedTime24Hour
                        binding.startTimeBtn.text = formattedTime12Hour
                    } else {
                        endTime = formattedTime24Hour
                        binding.endTimeBtn.text = formattedTime12Hour
                    }
                }
            },
            currentHour,
            currentMinute,
            false
        )

        timePickerDialog.setOnShowListener {
            val positiveButton = timePickerDialog.getButton(TimePickerDialog.BUTTON_POSITIVE)
            val negativeButton = timePickerDialog.getButton(TimePickerDialog.BUTTON_NEGATIVE)

            positiveButton.setTextColor(ContextCompat.getColor(this, R.color.blue))
            negativeButton.setTextColor(ContextCompat.getColor(this, R.color.blue))
        }

        timePickerDialog.show()
    }


    private fun showDatePicker(isStartDate: Boolean) {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            ContextThemeWrapper(this, R.style.CustomTimePickerDialog),
            { _, year, monthOfYear, dayOfMonth ->
                val selectedDate = Calendar.getInstance().apply {
                    set(year, monthOfYear, dayOfMonth)
                }

                val formattedDateDisplay = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault()).format(selectedDate.time)
                val formattedDateValue = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedDate.time)

                if (isStartDate) {
                    startDate = formattedDateValue
                    binding.startDateBtn.text = formattedDateDisplay
                } else {
                    endDate = formattedDateValue
                    binding.endDateBtn.text = formattedDateDisplay
                }
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        // Restrict the DatePicker to allow only future dates
        datePickerDialog.datePicker.minDate = System.currentTimeMillis()

        datePickerDialog.setOnShowListener {
            val positiveButton = datePickerDialog.getButton(DatePickerDialog.BUTTON_POSITIVE)
            val negativeButton = datePickerDialog.getButton(DatePickerDialog.BUTTON_NEGATIVE)
            positiveButton.setTextColor(ContextCompat.getColor(this, R.color.blue))
            negativeButton.setTextColor(ContextCompat.getColor(this, R.color.blue))
        }

        datePickerDialog.show()
    }


    private fun validateFields() {
        val eventName = binding.eventNameEt.text.toString().trim()
        val streamLink = binding.streamLinkEt.text.toString().trim()
        val description = binding.descriptionEt.text.toString().trim()

        if (coverImage.isEmpty()) {
            CustomSnackBar.showSnackBar(binding.root,MessageStore.selectImage(this))
            return
        }
        if (eventName.isEmpty()) {
            CustomSnackBar.showSnackBar(binding.root,MessageStore.pleaseEnterYourName(this))
            return
        }
        if (startDate.isEmpty()) {
            CustomSnackBar.showSnackBar(binding.root,MessageStore.selectStartDate(this))
            return
        }
        if (startTime.isEmpty()) {
            CustomSnackBar.showSnackBar(binding.root,MessageStore.selectStartTime(this))
            return
        }
        if (endDate.isEmpty()) {
            CustomSnackBar.showSnackBar(binding.root,MessageStore.selectEndDate(this))
            return
        }
        if (endTime.isEmpty()) {
            CustomSnackBar.showSnackBar(binding.root,MessageStore.selectEndTime(this))
            return
        }
        if (eventFormat.isEmpty()) {
            CustomSnackBar.showSnackBar(binding.root,MessageStore.selectEventFormat(this))
            return
        }
//        if (binding.streamLinkEt.isVisible && streamLink.isEmpty()) {
//            CustomSnackBar.showSnackBar(binding.root,MessageStore.enterStreamLink(this))
//            return
//        }
//        eventFormat = "online"
        if (eventFormat == "online"){
            if (streamLink.isEmpty()) {
                CustomSnackBar.showSnackBar(binding.root,MessageStore.enterStreamLink(this))
                return
            }
        }




        if (binding.venueEt.isVisible && venueAddress.isEmpty()) {
            CustomSnackBar.showSnackBar(binding.root,MessageStore.selectVenue(this))
            return
        }
        if (description.isEmpty()) {
            CustomSnackBar.showSnackBar(binding.root,MessageStore.pleaseEnterDescription(this))
            return
        }
        hideKeyboard()
        createEvent(eventName,startDate,startTime,endDate,endTime,eventFormat,streamLink,venueName,description)
    }

    private fun openGallery() {
        galleryLauncher.launch("image/*")
    }

    private fun createEvent(eventName: String, startDate: String, startTime: String, endDate: String, endTime: String, eventFormat: String, streamLink: String, venueName: String, description: String
    ) {
        val coverImageFile = File(coverImage)
        individualViewModal.createEvent(eventName,startDate,startTime,endDate,endTime,eventFormat,venueName,streamLink,description,venueAddress, eventLatitude.toString(),eventLongitude.toString(),coverImageFile)

    }



}