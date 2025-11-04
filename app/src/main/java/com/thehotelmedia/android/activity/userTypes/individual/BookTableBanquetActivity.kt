package com.thehotelmedia.android.activity.userTypes.individual

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.thehotelmedia.android.R
import com.thehotelmedia.android.ViewModelFactory
import com.thehotelmedia.android.activity.BaseActivity
import com.thehotelmedia.android.customClasses.ColorFilterTransformation
import com.thehotelmedia.android.customClasses.Constants.business_type_individual
import com.thehotelmedia.android.customClasses.CustomProgressBar
import com.thehotelmedia.android.customClasses.CustomSnackBar
import com.thehotelmedia.android.customClasses.PreferenceManager
import com.thehotelmedia.android.databinding.ActivityBookTableBanquetBinding
import com.thehotelmedia.android.extensions.capitalizeFirstLetter
import com.thehotelmedia.android.extensions.formatBookingDates
import com.thehotelmedia.android.extensions.getMealType
import com.thehotelmedia.android.extensions.navigateToMainActivity
import com.thehotelmedia.android.extensions.setRatingWithStarWithoutBracket
import com.thehotelmedia.android.modals.booking.bookingSummary.Data
import com.thehotelmedia.android.repository.IndividualRepo
import com.thehotelmedia.android.viewModal.individualViewModal.IndividualViewModal
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class BookTableBanquetActivity : BaseActivity() {

    private lateinit var binding: ActivityBookTableBanquetBinding

    private lateinit var progressBar: CustomProgressBar
    private lateinit var individualViewModal: IndividualViewModal
    private var bookingId = ""
    private var bookingType = ""
    private var from = ""

    private lateinit var preferenceManager : PreferenceManager
    private var businessesType: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBookTableBanquetBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up repository and view model
        val individualRepo = IndividualRepo(this)
        individualViewModal = ViewModelProvider(this, ViewModelFactory(null, individualRepo, null))[IndividualViewModal::class.java]
        // Initialize progress bar
        progressBar = CustomProgressBar(this)

        initUi()
    }

    private fun initUi() {
        binding.hasDataLayout.visibility = View.GONE


        preferenceManager = PreferenceManager.getInstance(this@BookTableBanquetActivity)
        businessesType = preferenceManager.getString(PreferenceManager.Keys.BUSINESS_TYPE,"").toString()

        bookingId = intent.getStringExtra("BOOKING_ID") ?: ""
        bookingType = intent.getStringExtra("BOOKING_TYPE") ?: ""
        from = intent.getStringExtra("FROM") ?: ""



        getBookingSummary()

        binding.backBtn.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.btnAccept.setOnClickListener {
            acceptRejectBooking("confirmed")
        }
        binding.btnReject.setOnClickListener {
            acceptRejectBooking("canceled by business")
        }

        individualViewModal.bookingSummaryResult.observe(this){result->
            if (result.status == true){
                binding.hasDataLayout.visibility = View.VISIBLE
                handelSummaryData(result.data)
            }else{
                val msg = result.message
                Toast.makeText(this,msg, Toast.LENGTH_SHORT).show()
            }
        }
        individualViewModal.acceptRejectTableBanquetResult.observe(this){result->
            if (result.status == true){
                navigateToMainActivity(businessesType == business_type_individual)
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

    private fun acceptRejectBooking(bookingStatus : String){
        individualViewModal.acceptRejectTableBanquet(bookingId,bookingStatus)
    }

    private fun handelSummaryData(data: Data?) {
        val bookingStatus = data?.status ?: ""
        val businessProfileRef = data?.businessProfileRef
        val businessIcon = businessProfileRef?.businessTypeRef?.icon ?: ""
        val userLargeProfilePic = businessProfileRef?.profilePic?.large ?: ""
        val hotelFullName = businessProfileRef?.name ?: ""
        val businessName = businessProfileRef?.businessTypeRef?.name ?: ""
        val rating = businessProfileRef?.rating ?: 0.0

        val address =  businessProfileRef?.address
        val street = address?.street ?: ""
        val city = address?.city ?: ""
        val state = address?.state ?: ""
        val country = address?.country ?: ""
        val zipCode = address?.zipCode ?: ""

        val fullAddress = if (street.isNotEmpty()) {
            "$street, $city, $state, $country, $zipCode"
        } else {
            "$city, $state, $country, $zipCode"
        }


        Glide.with(this).load(businessIcon)
            .placeholder(R.drawable.ic_hotel)
            .transform(ColorFilterTransformation(ContextCompat.getColor(this, R.color.white_40)))
            .into(binding.businessIconIv)
        Glide.with(this).load(userLargeProfilePic).placeholder(R.drawable.ic_profile_placeholder).into(binding.hotelProfileIv)
        binding.hotelNameTv.text = hotelFullName
        binding.addressTv.text = fullAddress
        binding.businessTypeTv.text = businessName
        binding.averageRatingTv.setRatingWithStarWithoutBracket(rating, R.drawable.ic_rating_star)

        val usersRef = data?.usersRef

        val userName = usersRef?.name ?: ""
        val userEmail = usersRef?.email ?: ""
        val userDialCode = usersRef?.dialCode ?: ""
        val userPhoneNumber = usersRef?.phoneNumber ?: ""

        if (userEmail.isEmpty()){
            binding.emailLayout.visibility = View.GONE
        }
        if (userPhoneNumber.isEmpty()){
            binding.numberLayout.visibility = View.GONE
        }

        binding.nameTv.text = userName
        binding.emailTv.text = userEmail
        binding.phoneNumberTv.text = "$userDialCode $userPhoneNumber"
        val createdAt = data?.createdAt ?: ""
        val bookingDate = formatDateForBookingDate(createdAt)
        binding.bookingDateTv.text = bookingDate

        when (bookingType) {
            "book-table" -> {
                binding.eventGuestLayout.visibility = View.GONE
                binding.tableBookingDetailsLayout.visibility = View.VISIBLE
                binding.eventBookingDetailsLayout.visibility = View.GONE

                val checkIn = data?.checkIn ?: ""
                val bookingForDate = formatDateBookingFor(checkIn)
                binding.bookingForTv.text = bookingForDate

                val adults = data?.adults ?: ""
                binding.tableGuestTv.text = adults.toString()

                val mealType = getMealType(bookingForDate)
                binding.tableTypeTv.text = mealType

                val iconResId = getEventIconResId(mealType)
                binding.tableTypeIv.setImageResource(iconResId)

            }else -> {
                binding.eventGuestLayout.visibility = View.VISIBLE
                binding.tableBookingDetailsLayout.visibility = View.GONE
                binding.eventBookingDetailsLayout.visibility = View.VISIBLE

                val eventGuestNo = data?.adults ?: ""
                binding.eventGuestTv.text = eventGuestNo.toString()

                val typeOfEvent = data?.metadata?.typeOfEvent ?: ""

                binding.eventTypeTv.text = typeOfEvent

                val iconResId = getEventIconResId(typeOfEvent)
                binding.eventTypeIv.setImageResource(iconResId)

                val checkInDate = data?.checkIn ?: ""
                val checkOutDate = data?.checkOut ?: ""

                val (formattedCheckIn, formattedCheckOut, nights) = formatBookingDates(checkInDate, checkOutDate)
//                binding.noOfNightTv.text = "${nights}${getString(R.string.night)}"
                binding.checkInDateTv.text = formattedCheckIn
                binding.checkOutDateTv.text = formattedCheckOut

            }
        }

        if (from == "NOTIFICATION" && bookingStatus == "pending"){
            if (businessesType == business_type_individual){
                binding.bookingStatusLayout.visibility = View.GONE
            }else{
                binding.bookingStatusLayout.visibility = View.VISIBLE
            }
        }else{
            binding.bookingStatusLayout.visibility = View.GONE
        }

        if (from == "NOTIFICATION" && bookingStatus != "pending"){
            when (bookingStatus) {
                "confirmed" -> {
                    binding.statusLayout.setBackgroundResource(R.drawable.background_6_green)
                }
                else -> {
                    binding.statusLayout.setBackgroundResource(R.drawable.background_6_red)
                }
            }
            binding.statusLayout.visibility = View.VISIBLE
            binding.statusTv.text = bookingStatus.capitalizeFirstLetter()
        }else{
            binding.statusLayout.visibility = View.GONE
        }




    }



    private fun getBookingSummary() {
        individualViewModal.getBookingSummary(bookingId)
    }

    private fun formatDateBookingFor(isoDate: String): String {
        val inputFormatter = DateTimeFormatter.ISO_ZONED_DATE_TIME
        val outputFormatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy | h:mma", Locale.ENGLISH)

        val zonedDateTime = ZonedDateTime.parse(isoDate, inputFormatter)
        return outputFormatter.format(zonedDateTime).replace("AM", "Am").replace("PM", "Pm")
    }

    private fun formatDateForBookingDate(isoDate: String): String {
        val inputFormatter = DateTimeFormatter.ISO_ZONED_DATE_TIME
        val outputFormatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy | h:mma", Locale.ENGLISH)

        val zonedDateTime = ZonedDateTime.parse(isoDate, inputFormatter)
            .withZoneSameInstant(ZoneId.systemDefault()) // convert to local timezone

        return outputFormatter.format(zonedDateTime).replace("AM", "Am").replace("PM", "Pm")
    }


    private fun getEventIconResId(occasion: String): Int {
        return when (occasion) {
            "Birthday Party" -> R.drawable.birthday_party
            "Wedding Ceremony" -> R.drawable.wedding_ceremony
            "Anniversary Celebration" -> R.drawable.anniversary_celebration
            "Corporate Event" -> R.drawable.corporate_event
            "Baby Shower" -> R.drawable.baby_shower
            "Engagement Party" -> R.drawable.engagement_party
            "Farewell Party" -> R.drawable.farewell_party
            "Reunion" -> R.drawable.reunion
            "Festival Celebration" -> R.drawable.festival_celebration
            "Charity Event" -> R.drawable.charity_event
            "Other Occasion" -> R.drawable.other_occasion
            "Breakfast" -> R.drawable.ic_breakfast
            "Lunch" -> R.drawable.ic_lunch
            "Dinner" -> R.drawable.ic_dinner
            else -> R.drawable.other_occasion
        }
    }
}