package com.thehotelmedia.android.activity.booking

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.thehotelmedia.android.R
import com.thehotelmedia.android.ViewModelFactory
import com.thehotelmedia.android.activity.BaseActivity
import com.thehotelmedia.android.adapters.BookBanquetAdapter
import com.thehotelmedia.android.adapters.dropDown.BusinessTypeAdapter
import com.thehotelmedia.android.adapters.dropDown.Businesses
import com.thehotelmedia.android.customClasses.ColorFilterTransformation
import com.thehotelmedia.android.customClasses.Constants.business_type_individual
import com.thehotelmedia.android.customClasses.CustomProgressBar
import com.thehotelmedia.android.customClasses.CustomSnackBar
import com.thehotelmedia.android.customClasses.PreferenceManager
import com.thehotelmedia.android.customClasses.SuccessGiff
import com.thehotelmedia.android.databinding.ActivityBookBanquetBinding
import com.thehotelmedia.android.extensions.navigateToMainActivity
import com.thehotelmedia.android.extensions.setRatingWithStarWithoutBracket
import com.thehotelmedia.android.extensions.showDatePicker
import com.thehotelmedia.android.extensions.showToast
import com.thehotelmedia.android.repository.IndividualRepo
import com.thehotelmedia.android.viewModal.individualViewModal.IndividualViewModal
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class BookBanquetActivity : BaseActivity() {

    private lateinit var binding: ActivityBookBanquetBinding
    private var checkInDate: String? = null
    private var checkOutDate: String? = null
    private var checkInCalendar: Calendar? = null  // Store check-in date
    private var selectedNoOfGuest = ""
    private var eventType = ""
    private var businessesType: String = ""
    private lateinit var successGiff: SuccessGiff
    private lateinit var progressBar: CustomProgressBar
    private lateinit var individualViewModal: IndividualViewModal
    private lateinit var preferenceManager : PreferenceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBookBanquetBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up repository and view model
        val individualRepo = IndividualRepo(this)
        individualViewModal = ViewModelProvider(this, ViewModelFactory(null, individualRepo, null))[IndividualViewModal::class.java]
        // Initialize progress bar
        progressBar = CustomProgressBar(this)
        successGiff = SuccessGiff(this)
        preferenceManager = PreferenceManager.getInstance(this@BookBanquetActivity)
        businessesType = preferenceManager.getString(PreferenceManager.Keys.BUSINESS_TYPE,"").toString()

        initUi()
    }

    private fun initUi() {
        val businessProfileId = intent.getStringExtra("KEY_BUSINESS_PROFILE_ID") ?: ""
        val userLargeProfilePic = intent.getStringExtra("KEY_USER_LARGE_PROFILE_PIC") ?: ""
        val userFullName = intent.getStringExtra("KEY_USER_FULL_NAME") ?: ""
        val businessName = intent.getStringExtra("KEY_BUSINESS_NAME") ?: ""
        val businessIcon = intent.getStringExtra("KEY_BUSINESS_ICON") ?: ""
        val fullAddress = intent.getStringExtra("KEY_FULL_ADDRESS") ?: ""
        val rating = intent.getDoubleExtra("KEY_RATING", 0.0) // Default to 0.0 if not found

        Glide.with(this).load(businessIcon)
            .placeholder(R.drawable.ic_hotel)
            .transform(ColorFilterTransformation(ContextCompat.getColor(this, R.color.white_40)))
            .into(binding.businessIconIv)

        Glide.with(this).load(userLargeProfilePic).placeholder(R.drawable.ic_profile_placeholder).into(binding.hotelProfileIv)
        binding.hotelNameTv.text = userFullName
        binding.addressTv.text = fullAddress
        binding.businessTypeTv.text = businessName
        binding.averageRatingTv.setRatingWithStarWithoutBracket(rating, R.drawable.ic_rating_star)

        setDropDown()

        setRecycleView()

        binding.backBtn.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.checkInBtn.setOnClickListener {
            showDatePicker(
                isCheckInDate = true,
                checkInCalendar = checkInCalendar,
                checkOutDate = checkOutDate
            ) { selectedDate, selectedCalendar ->
                checkInDate = selectedDate
                checkInCalendar = selectedCalendar
                binding.checkInBtn.text = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault()).format(selectedCalendar.time)
                binding.checkOutBtn.setTextColor(ContextCompat.getColor(this, R.color.text_color))
                checkOutDate = null
                binding.checkOutBtn.text = ""

            }
        }

        binding.checkOutBtn.setOnClickListener {

            if (checkInDate == null) {
                // Show toast if user tries to select check-out before check-in
                Toast.makeText(this, getString(R.string.select_checkin_date_first), Toast.LENGTH_SHORT).show()
            } else {
                showDatePicker(
                    isCheckInDate = false,
                    checkInCalendar = checkInCalendar,
                    checkOutDate = checkOutDate
                ) { selectedDate, _ ->
                    checkOutDate = selectedDate
                    binding.checkOutBtn.text = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault()).format(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(selectedDate)!!)
                }
            }
        }

        binding.submitBtn.setOnClickListener {
            val context = binding.root.context

            if (binding.otherOccasionEt.visibility == View.VISIBLE) {
                eventType = binding.otherOccasionEt.text.toString().trim()
                if (eventType.isEmpty()) {
                    CustomSnackBar.showSnackBar(binding.root, context.getString(R.string.enter_occasion_type))
                    return@setOnClickListener
                }
            } else if (eventType.isEmpty()) {
                CustomSnackBar.showSnackBar(binding.root, context.getString(R.string.select_event_type))
                return@setOnClickListener
            }

            if (checkInDate.isNullOrEmpty()) {
                CustomSnackBar.showSnackBar(binding.root, context.getString(R.string.select_checkin_date))
                return@setOnClickListener
            }

            if (checkOutDate.isNullOrEmpty()) {
                CustomSnackBar.showSnackBar(binding.root, context.getString(R.string.select_checkout_date))
                return@setOnClickListener
            }

            if (selectedNoOfGuest.isEmpty()) {
                CustomSnackBar.showSnackBar(binding.root, context.getString(R.string.select_no_of_guest))
                return@setOnClickListener
            }

            println("Event Type: $eventType")
            println("Check-In Date: $checkInDate")
            println("Check-Out Date: $checkOutDate")
            println("Selected No. of Guests: $selectedNoOfGuest")
            val maxGuestValue = getMaxValue(selectedNoOfGuest)
            individualViewModal.bookABanquet(checkInDate.toString(),checkOutDate.toString(),businessProfileId,maxGuestValue,eventType)
        }

        individualViewModal.bookABanquetResult.observe(this@BookBanquetActivity){result->
            if (result.status==true){
                result.message?.let { msg ->
                    runOnUiThread {
                        successGiff.show(msg) {
                            navigateToMainActivity(businessesType == business_type_individual)
                        }
                    }
                }
            }else{
                val msg = result.message.toString()
                showToast(msg)
            }
        }

        individualViewModal.loading.observe(this@BookBanquetActivity){
            if (it == true){
                progressBar.show()
            }else{
                progressBar.hide()
            }
        }

        individualViewModal.toast.observe(this@BookBanquetActivity){
            CustomSnackBar.showSnackBar(binding.root,it)
        }

    }

    private fun setRecycleView() {
        val occasionsList = listOf(
            "Birthday Party",
            "Wedding Ceremony",
            "Anniversary Celebration",
            "Corporate Event",
            "Baby Shower",
            "Engagement Party",
            "Farewell Party",
            "Reunion",
            "Festival Celebration",
            "Charity Event",
            "Other Occasion"
        )

        val featuresAdapter = BookBanquetAdapter(this,occasionsList,::onEventTypeSelected)
        binding.featuresRv.adapter = featuresAdapter
    }

    private fun onEventTypeSelected(event: String) {
        if (event == "Other Occasion"){
            binding.otherOccasionEt.visibility = View.VISIBLE
        }else{
            eventType = event
            binding.otherOccasionEt.visibility = View.GONE
        }
    }

    private fun setDropDown() {

        binding.noOfGuestTv.setDropDownBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.blured_background))
        binding.noOfGuestTv.dropDownVerticalOffset = binding.noOfGuestTv.height + 30

        val guestRange = listOf(
            Businesses("0 - 50", "", "1"),
            Businesses("50 - 100", "", "2"),
            Businesses("100 - 200", "", "3"),
            Businesses("200 - 500", "", "4"),
            Businesses("500+", "", "5")
        )

        val businessTypeAdapter = BusinessTypeAdapter(this, guestRange)
        binding.noOfGuestTv.setAdapter(businessTypeAdapter)
        binding.noOfGuestTv.setOnItemClickListener { parent, _, position, _ ->
            val selectedItem = parent.getItemAtPosition(position) as Businesses
            binding.noOfGuestTv.setTextColor(ContextCompat.getColor(this, R.color.text_color))
            selectedNoOfGuest = selectedItem.name
//            val selectBusinessId = selectedItem.id
            println("Selected range: ${selectedItem.name}")
        }

    }

    private fun getMaxValue(guestRange: String): Int {
        return if (guestRange.contains("+")) {
            // Agar "500+" hai, to "+" hata ke integer convert karein
            guestRange.replace("+", "").trim().toInt()
        } else {
            // Range ko split karke max value nikalein
            guestRange.split("-")[1].trim().toInt()
        }
    }

}