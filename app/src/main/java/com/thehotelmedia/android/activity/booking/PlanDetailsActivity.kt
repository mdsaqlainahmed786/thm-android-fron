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
import com.thehotelmedia.android.adapters.booking.HotelRoomsAdapter
import com.thehotelmedia.android.customClasses.ColorFilterTransformation
import com.thehotelmedia.android.customClasses.CustomProgressBar
import com.thehotelmedia.android.customClasses.CustomSnackBar
import com.thehotelmedia.android.databinding.ActivityPlanDetailsBinding
import com.thehotelmedia.android.extensions.setRatingWithStarWithoutBracket
import com.thehotelmedia.android.repository.IndividualRepo
import com.thehotelmedia.android.viewModal.individualViewModal.IndividualViewModal
import java.text.SimpleDateFormat
import java.util.Locale

class PlanDetailsActivity : BaseActivity() {

    private lateinit var binding: ActivityPlanDetailsBinding
    private var childrenAges: List<Int> = emptyList() // Immutable list

    // Declare variables at the top
    private var businessProfileId: String = ""
    private var userLargeProfilePic: String = ""
    private var userFullName: String = ""
    private var businessName: String = ""
    private var businessIcon: String = ""
    private var fullAddress: String = ""
    private var rating: Double = 0.0
    private var checkInDate: String = ""
    private var checkOutDate: String = ""
    private var guestCount: Int = 0
    private var childrenCount: Int = 0
    private var hasPet: Boolean = false
    private var guestMessage: String = ""

    private lateinit var hotelRoomsAdapter: HotelRoomsAdapter


    private lateinit var individualViewModal: IndividualViewModal
    private lateinit var progressBar: CustomProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityPlanDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val individualRepo = IndividualRepo(this)
        individualViewModal = ViewModelProvider(this, ViewModelFactory(null, individualRepo, null))[IndividualViewModal::class.java]
        progressBar = CustomProgressBar(this)

        initVariables()
        initUI()
    }

    private fun initVariables() {
        businessProfileId = intent.getStringExtra("KEY_BUSINESS_PROFILE_ID") ?: ""
        userLargeProfilePic = intent.getStringExtra("KEY_USER_LARGE_PROFILE_PIC") ?: ""
        userFullName = intent.getStringExtra("KEY_USER_FULL_NAME") ?: ""
        businessName = intent.getStringExtra("KEY_BUSINESS_NAME") ?: ""
        businessIcon = intent.getStringExtra("KEY_BUSINESS_ICON") ?: ""
        fullAddress = intent.getStringExtra("KEY_FULL_ADDRESS") ?: ""
        rating = intent.getDoubleExtra("KEY_RATING", 0.0)
        checkInDate = intent.getStringExtra("KEY_CHECK_IN_DATE") ?: ""
        checkOutDate = intent.getStringExtra("KEY_CHECK_OUT_DATE") ?: ""
        guestCount = intent.getIntExtra("KEY_GUEST_COUNT", 0)
        childrenCount = intent.getIntExtra("KEY_CHILDREN_COUNT", 0)
        hasPet = intent.getBooleanExtra("KEY_HAS_PET", false)
        guestMessage = intent.getStringExtra("KEY_GUEST_MESSAGE") ?: ""

        childrenAges = intent.getIntegerArrayListExtra("KEY_CHILDREN_AGES")?.toList() ?: emptyList()

    }

    override fun onResume() {
        getAvailableRoomsData()
        super.onResume()
    }

    private fun initUI() {
        Glide.with(this).load(businessIcon)
            .placeholder(R.drawable.ic_hotel)
            .transform(ColorFilterTransformation(ContextCompat.getColor(this, R.color.white_40)))
            .into(binding.businessIconIv)
        Glide.with(this).load(userLargeProfilePic).placeholder(R.drawable.ic_profile_placeholder).into(binding.hotelProfileIv)
        binding.hotelNameTv.text = userFullName
        binding.addressTv.text = fullAddress
        binding.businessTypeTv.text = businessName
        binding.averageRatingTv.setRatingWithStarWithoutBracket(rating, R.drawable.ic_rating_star)
        binding.guestBtn.text = guestMessage
        binding.checkInBtn.text = formatDate(checkInDate)
        binding.checkOutBtn.text = formatDate(checkOutDate)





        binding.backBtn.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }



        individualViewModal.bookingCheckInResult.observe(this){result->
            if (result.status == true){

                val availableRoomsList = result.data?.availableRooms
                val bookingID = result.data?.booking?.bookingID ?: ""
                val checkInDate = result.data?.booking?.checkIn ?: ""
                val checkOutDate = result.data?.booking?.checkOut ?: ""
                val roomsRequired = result.data?.roomsRequired ?: 1

                val userData = result.data?.user
                 val isNumberVerified = userData?.mobileVerified ?: true
                 val phoneNumber = userData?.phoneNumber ?: ""
                 val dialCode = userData?.dialCode ?: ""


                if (!availableRoomsList.isNullOrEmpty()){
                    hotelRoomsAdapter = HotelRoomsAdapter(this,availableRoomsList,bookingID,checkInDate,checkOutDate,roomsRequired,guestCount,dialCode,phoneNumber,isNumberVerified)
                    binding.roomsRv.adapter = hotelRoomsAdapter
                    binding.availableRoomTv.visibility = View.VISIBLE
                    binding.roomsRv.visibility = View.VISIBLE
                    binding.noDataFoundLayout.visibility = View.GONE
                }else{
                    binding.availableRoomTv.visibility = View.GONE
                    binding.roomsRv.visibility = View.GONE
                    binding.noDataFoundLayout.visibility = View.VISIBLE
                    // Backend can return success with an empty list when filters (occupancy/dates/inventory) exclude all rooms.
                    // Show the server message to make troubleshooting possible.
                    val msg = result.message ?: getString(R.string.no_room_available)
                    CustomSnackBar.showSnackBar(binding.root, msg)
                }

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

    private fun getAvailableRoomsData() {
        individualViewModal.bookingCheckIn(businessProfileId,checkInDate,checkOutDate, guestCount,childrenCount,childrenAges,hasPet)
    }


    private fun formatDate(dateStr: String): String {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val outputFormat = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault())

        val date = inputFormat.parse(dateStr)
        return date?.let { outputFormat.format(it) } ?: ""
    }
}
