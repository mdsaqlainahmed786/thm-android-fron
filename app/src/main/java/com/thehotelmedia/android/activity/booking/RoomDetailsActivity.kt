package com.thehotelmedia.android.activity.booking

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import com.google.gson.Gson
import com.thehotelmedia.android.R
import com.thehotelmedia.android.ViewModelFactory
import com.thehotelmedia.android.activity.BaseActivity
import com.thehotelmedia.android.activity.booking.bottomSheet.SeeAllAmenitiesBottomSheetFragment
import com.thehotelmedia.android.adapters.booking.FeaturesAdapter
import com.thehotelmedia.android.adapters.booking.PropertyImageAdapter
import com.thehotelmedia.android.adapters.booking.SpokenLanguageAdapter
import com.thehotelmedia.android.customClasses.CustomProgressBar
import com.thehotelmedia.android.customClasses.CustomSnackBar
import com.thehotelmedia.android.customDialog.OtpDialogManager
import com.thehotelmedia.android.databinding.ActivityRoomDetailsBinding
import com.thehotelmedia.android.extensions.convertTo12HourFormat
import com.thehotelmedia.android.extensions.formatBookingDates
import com.thehotelmedia.android.extensions.setRoomTypeImage
import com.thehotelmedia.android.extensions.toggleEnable
import com.thehotelmedia.android.modals.booking.roomDetails.AmenitiesRef
import com.thehotelmedia.android.repository.IndividualRepo
import com.thehotelmedia.android.viewModal.individualViewModal.IndividualViewModal

class RoomDetailsActivity : BaseActivity() {

    private lateinit var binding: ActivityRoomDetailsBinding
    private var roomCount = 1
    private var minRoomRequirement = 1

    private lateinit var individualViewModal: IndividualViewModal
    private lateinit var progressBar: CustomProgressBar

    private var roomId = ""
    private var bookingId = ""
    private var checkInDate = ""
    private var checkOutDate = ""
    private var pricePerNight = 0.0
    private var guestCount = 0


    private var dialCode = ""
    private var phoneNumber = ""
    private var isNumberVerified = false

    private var amenitiesList: ArrayList<AmenitiesRef> = arrayListOf()

    private lateinit var otpDialogManager: OtpDialogManager


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRoomDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val individualRepo = IndividualRepo(this)
        individualViewModal = ViewModelProvider(this, ViewModelFactory(null, individualRepo, null))[IndividualViewModal::class.java]
        progressBar = CustomProgressBar(this)

        otpDialogManager = OtpDialogManager(this)

        initUi()
    }

    private fun initUi() {

        roomId = intent.getStringExtra("ROOM_ID") ?: ""  // Default empty string if null
        bookingId = intent.getStringExtra("BOOKING_ID") ?: "" // Default empty string if null
        checkInDate = intent.getStringExtra("CHECK_IN_DATE") ?: "" // Default empty string if null
        checkOutDate = intent.getStringExtra("CHECK_OUT_DATE") ?: "" // Default empty string if null
        pricePerNight = intent.getDoubleExtra("PRICE_PER_NIGHT",0.0)


        roomCount = intent.getIntExtra("ROOM_REQUIRED",0)
        minRoomRequirement = intent.getIntExtra("ROOM_REQUIRED",0)
        guestCount = intent.getIntExtra("GUEST_COUNT",0)


        dialCode = intent.getStringExtra("DIAL_CODE") ?: ""
        phoneNumber = intent.getStringExtra("PHONE_NUMBER") ?: ""
        isNumberVerified = intent.getBooleanExtra("IS_NUMBER_VERIFIED",false)

        println("adsfjksdhakj  dialCode  $dialCode")
        println("adsfjksdhakj  phoneNumber  $phoneNumber")
        println("adsfjksdhakj  isNumberVerified  $isNumberVerified")



        setBottomButtonData()



        binding.roomNumberTv.text = roomCount.toString()
        binding.decBtn.toggleEnable(roomCount > minRoomRequirement)


        getRoomDetails()



        val layoutManager = FlexboxLayoutManager(this).apply {
            flexDirection = FlexDirection.ROW
            justifyContent = JustifyContent.FLEX_START
        }
        binding.languageRv.layoutManager = layoutManager



        binding.backBtn.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }


        binding.nextBtn.setOnClickListener {
            if (isNumberVerified){
                moveToBookingConfirmationScreen()
            }else{
                val initialDialCode = if (dialCode.isNotEmpty()) dialCode else "+91"
                otpDialogManager.startPhoneVerificationFlow(initialDialCode, phoneNumber) { finalDialCode, finalPhoneNumber ->
                    isNumberVerified = true
                    dialCode = finalDialCode
                    phoneNumber = finalPhoneNumber
//                        moveToBookingConfirmationScreen()
                }
            }


        }





        binding.addBtn.setOnClickListener {
            roomCount++
            binding.roomNumberTv.text = roomCount.toString()
            binding.decBtn.toggleEnable(true)
//            binding.applyBtn.text = getString(R.string.next)
            calculatePrice()
        }

        binding.decBtn.setOnClickListener {
            if (roomCount > minRoomRequirement) {
                roomCount--
                binding.roomNumberTv.text = roomCount.toString()
                binding.decBtn.toggleEnable(roomCount > minRoomRequirement)
                calculatePrice()
            }
        }


//        binding.seeAllAmenities.setOnClickListener {
//            val bottomSheetFragment = SeeAllAmenitiesBottomSheetFragment()
//            bottomSheetFragment.show(supportFragmentManager, bottomSheetFragment.tag)
//        }

        binding.seeAllAmenities.setOnClickListener {
            val gson = Gson()
            val jsonAmenitiesRef = gson.toJson(amenitiesList) // Convert list to JSON

            val bottomSheetFragment = SeeAllAmenitiesBottomSheetFragment()
            val bundle = Bundle()
            bundle.putString("amenities_json", jsonAmenitiesRef) // Pass JSON as argument
            bottomSheetFragment.arguments = bundle

            bottomSheetFragment.show(supportFragmentManager, bottomSheetFragment.tag)
        }



        individualViewModal.roomDetailsResult.observe(this){result->
            if (result.status == true){

                val data = result.data
                val coverImage = data?.cover?.sourceUrl ?: ""
                val businessProfileID = data?.businessProfileID ?: ""
                val title = data?.title ?: ""
                val description = data?.description ?: ""
                val bedType = data?.bedType ?: ""

                val roomImagesRef = data?.roomImagesRef
                val checkInTime = data?.checkIn ?: ""
                val checkOutTime = data?.checkOut ?: ""
                val languageSpoken = data?.languageSpoken
                amenitiesList = data?.amenitiesRef ?: arrayListOf()

                val itemList = amenitiesList.mapNotNull { it.name }
                if (itemList.isNotEmpty()){
                    if (itemList.size <= 5){
                        binding.seeAllAmenities.visibility = View.GONE
                    }else{
                        binding.seeAllAmenities.visibility = View.VISIBLE
                    }
                    val featuresAdapter = FeaturesAdapter(itemList,true)
                    binding.featuresRv.adapter = featuresAdapter
                }else{
                    binding.seeAllAmenities.visibility = View.GONE
                }

                Glide.with(this).load(coverImage).placeholder(R.drawable.ic_post_placeholder).into(binding.coverIv)
                binding.descriptionTv.text = description
                binding.roomTitletv.text = title
                binding.checkInTime.text = convertTo12HourFormat(checkInTime)
                binding.checkOutTime.text = convertTo12HourFormat(checkOutTime)

                binding.roomTypeIv.setRoomTypeImage(bedType)

                if (!roomImagesRef.isNullOrEmpty()){
                    val propertyImageAdapter = PropertyImageAdapter(this,roomImagesRef)
                    binding.propertyImageRv.adapter = propertyImageAdapter
                }


                if (!languageSpoken.isNullOrEmpty()){
                    val spokenLanguageAdapter = SpokenLanguageAdapter(this,languageSpoken)
                    binding.languageRv.adapter = spokenLanguageAdapter
                }





            }else{
                val msg = result.message
                Toast.makeText(this,msg, Toast.LENGTH_SHORT).show()
            }
        }


        individualViewModal.loading.observe(this){

            println("hjkhjkhjk    Loading data")
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

    private fun moveToBookingConfirmationScreen() {
        val intent = Intent(this, BookingConfirmationActivity::class.java).apply {
            putExtra("ROOM_ID", roomId) // Assuming roomId is a String
            putExtra("BOOKING_ID", bookingId) // Assuming bookingId is a String
            putExtra("ROOM_COUNT", roomCount) // Assuming roundCount is an Int
            putExtra("GUEST_COUNT", guestCount) // Assuming roundCount is an Int
            putExtra("PRICE_PER_NIGHT", pricePerNight) // Assuming roundCount is an Int
        }
        startActivity(intent)
    }


    private fun getRoomDetails() {
        individualViewModal.fetchRoomDetails(roomId)
    }

    private fun setBottomButtonData() {

        println("asfdjlsadkjgkl.   checkInDate.   $checkInDate")
        println("asfdjlsadkjgkl.   checkOutDate.   $checkOutDate")

        val (formattedCheckIn, formattedCheckOut, nights) = formatBookingDates(checkInDate, checkOutDate)

        binding.checkInCheckOutDate.text = "$formattedCheckIn - $formattedCheckOut - ${nights}Night"

        calculatePrice()


    }

    private fun calculatePrice() {
        val totalPrice = pricePerNight * roomCount
        binding.priceTv.text = "₹${pricePerNight.toInt()} × $roomCount = ₹${totalPrice.toInt()}"
    }



}