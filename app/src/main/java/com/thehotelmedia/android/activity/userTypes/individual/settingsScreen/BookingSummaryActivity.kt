package com.thehotelmedia.android.activity.userTypes.individual.settingsScreen

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.thehotelmedia.android.R
import com.thehotelmedia.android.ViewModelFactory
import com.thehotelmedia.android.activity.BaseActivity
import com.thehotelmedia.android.adapters.booking.GuestListAdapter
import com.thehotelmedia.android.bottomSheets.YesOrNoBottomSheetFragment
import com.thehotelmedia.android.customClasses.ColorFilterTransformation
import com.thehotelmedia.android.customClasses.CustomProgressBar
import com.thehotelmedia.android.customClasses.CustomSnackBar
import com.thehotelmedia.android.databinding.ActivityBookingSummaryBinding
import com.thehotelmedia.android.downloadManager.FileDownloadManager
import com.thehotelmedia.android.extensions.blurTheView
import com.thehotelmedia.android.extensions.capitalizeFirstLetter
import com.thehotelmedia.android.extensions.formatBookingDates
import com.thehotelmedia.android.extensions.roundToTwoDecimal
import com.thehotelmedia.android.extensions.setRatingWithStarWithoutBracket
import com.thehotelmedia.android.extensions.setRoomTypeImage
import com.thehotelmedia.android.extensions.toFormattedDateTime
import com.thehotelmedia.android.modals.booking.bookingSummary.Data
import com.thehotelmedia.android.repository.IndividualRepo
import com.thehotelmedia.android.viewModal.individualViewModal.IndividualViewModal

class BookingSummaryActivity : BaseActivity() {

    private lateinit var binding: ActivityBookingSummaryBinding
    private lateinit var guestListAdapter : GuestListAdapter
    private lateinit var progressBar: CustomProgressBar
    private lateinit var individualViewModal: IndividualViewModal
    private var bookingId = ""
    private lateinit var fileDownloadManager: FileDownloadManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBookingSummaryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up repository and view model
        val individualRepo = IndividualRepo(this)
        individualViewModal = ViewModelProvider(this, ViewModelFactory(null, individualRepo, null))[IndividualViewModal::class.java]
        // Initialize progress bar
        progressBar = CustomProgressBar(this)
        fileDownloadManager = FileDownloadManager(this)
        initUi()
    }

    private fun initUi() {
        binding.hasDataLayout.visibility = View.GONE
        bookingId = intent.getStringExtra("BOOKING_ID") ?: ""

        blurTheView(binding.downloadInvoiceBlurView)
        blurTheView(binding.cancelBookingBlurView)

        getBookingSummary()




        binding.backBtn.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.cancelBookingBtn.setOnClickListener{
            // Show the BottomSheetFragment
            val bottomSheet = YesOrNoBottomSheetFragment.newInstance(getString(R.string.really_want_to_cancel_booking))
            bottomSheet.onYesClicked = {
                individualViewModal.cancelBooking(bookingId)
            }
            bottomSheet.onNoClicked = {

            }
            bottomSheet.show(supportFragmentManager, "YesOrNoBottomSheet")
        }

        binding.downloadInvoiceBtn.setOnClickListener {
            // Show the BottomSheetFragment
            val bottomSheet = YesOrNoBottomSheetFragment.newInstance(getString(R.string.want_to_download_invoice))
            bottomSheet.onYesClicked = {
                individualViewModal.downloadBookingInvoice(bookingId)
            }
            bottomSheet.onNoClicked = {

            }
            bottomSheet.show(supportFragmentManager, "YesOrNoBottomSheet")
        }


        individualViewModal.bookingSummaryResult.observe(this){result->
            if (result.status == true){
                binding.hasDataLayout.visibility = View.VISIBLE
                handelBookingSummaryData(result.data)
            }else{
                val msg = result.message
                Toast.makeText(this,msg, Toast.LENGTH_SHORT).show()
            }
        }
        individualViewModal.downloadBookingInvoiceResult.observe(this){result->
            if (result.status == true){

                val data = result.data
                val fileName = data?.filename ?: "THM_INVOICE.pdf"
                val filePath = data?.filePath ?: ""

                println("sadfjklsadjfkas    filePath $filePath")

                if (filePath.isNotEmpty()){
                    fileDownloadManager.downloadFile(fileName, filePath)
                }else{
                    Toast.makeText(this,"Unable to get get invoice",Toast.LENGTH_SHORT).show()
                }

            }else{
                val msg = result.message
                Toast.makeText(this,msg, Toast.LENGTH_SHORT).show()
            }
        }


        individualViewModal.cancelBookingResult.observe(this){result->
            if (result.status==true){
                getBookingSummary()
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

    private fun handelBookingSummaryData(data: Data?) {

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
        val guestList = data?.guestDetails

        if (!guestList.isNullOrEmpty()){
            guestListAdapter = GuestListAdapter(this,guestList)
            binding.guestListRv.adapter = guestListAdapter
        }

        val bookingId = data?.bookingID ?: ""
        val createdAt = data?.createdAt ?: ""
        val paymentMethod = data?.paymentDetail?.paymentMethod ?: "Unknown"

        binding.bookingIdTv.text = bookingId
        binding.paymentTypeTv.text = paymentMethod.capitalizeFirstLetter()
        binding.dateTimeTv.text = createdAt.toFormattedDateTime()

        val roomQuantity = data?.bookedRoom?.quantity ?: 1

        binding.roomQuantityTv.text = "$roomQuantity ${getString(R.string.room)}"


        Glide.with(this).load(businessIcon)
            .placeholder(R.drawable.ic_hotel)
            .transform(ColorFilterTransformation(ContextCompat.getColor(this, R.color.white_40)))
            .into(binding.businessIconIv)
        Glide.with(this).load(userLargeProfilePic).placeholder(R.drawable.ic_profile_placeholder).into(binding.hotelProfileIv)
        binding.hotelNameTv.text = hotelFullName
        binding.addressTv.text = fullAddress
        binding.businessTypeTv.text = businessName
        binding.averageRatingTv.setRatingWithStarWithoutBracket(rating, R.drawable.ic_rating_star)


        val freeCancelBy = data?.freeCancelBy ?: ""
        val freeCancel = data?.freeCancel ?: false

        if (freeCancel){
            binding.freeCancelationLayout.visibility = View.VISIBLE
            binding.cancelBookingBtn.visibility = View.VISIBLE
            binding.downloadInvoiceBtn.visibility = View.GONE
        }else{
            binding.freeCancelationLayout.visibility = View.GONE
            binding.cancelBookingBtn.visibility = View.GONE
            binding.downloadInvoiceBtn.visibility = View.VISIBLE
        }

        binding.freeCancelTv.text = "${getString(R.string.free_cancellation_till)} ${freeCancelBy.toFormattedDateTime()}"

        val gstPrice = data?.tax
        val gstPercentage = data?.gstRate ?: 0
        val subtotal = data?.subTotal
        val convinceCharges = data?.convinceCharge
        val discount = data?.discount ?: 0
        val promoCode = data?.promoCode
        val grandTotal = data?.grandTotal

        val usersRef = data?.usersRef

        val userEmail = usersRef?.email ?: ""
        val userDialCode = usersRef?.dialCode ?: ""
        val userPhoneNumber = usersRef?.phoneNumber ?: ""

        binding.emailTv.text = userEmail
        binding.phoneNumberTv.text = "$userDialCode $userPhoneNumber"

        val roomType = data?.roomsRef?.title ?: ""
        val bedType =  data?.roomsRef?.bedType ?: ""

        binding.roomTypeIv.setRoomTypeImage(bedType)

        binding.roomTypeTv.text = roomType
        binding.billingRoomType.text = roomType

        val checkInDate = data?.checkIn ?: ""
        val checkOutDate = data?.checkOut ?: ""

        val (formattedCheckIn, formattedCheckOut, nights) = formatBookingDates(checkInDate, checkOutDate)
        binding.noOfNightTv.text = "${nights}${getString(R.string.night)}"
        binding.checkInDateTv.text = formattedCheckIn
        binding.checkOutDateTv.text = formattedCheckOut

        binding.planChargesTv.text = "₹${subtotal.roundToTwoDecimal()}"
        binding.convinceChargesPriceTv.text = "₹${convinceCharges.roundToTwoDecimal()}"
        binding.gstPercentageTv.text = "${getString(R.string.gst)}($gstPercentage%)"
        binding.gstPriceTv.text = "₹$gstPrice"
        binding.totalPriceTv.text = "₹${grandTotal.roundToTwoDecimal()}"

        if (discount != 0){
            binding.promoCodeLayout.visibility = View.VISIBLE
        }else{
            binding.promoCodeLayout.visibility = View.GONE
        }

        binding.promoCodeNameTv.text = promoCode
        binding.promoCodeTv.text = "-₹$discount"


        if (bookingStatus== "canceled" || bookingStatus == "canceled by business"){
            binding.freeCancelationLayout.visibility = View.GONE
            binding.cancelBookingBtn.visibility = View.GONE
            binding.downloadInvoiceBtn.visibility = View.GONE
            binding.bookingCanceledLayout.visibility = View.VISIBLE
            binding.bookingCancelTv.text = bookingStatus.capitalizeFirstLetter()

        }





    }

    private fun getBookingSummary() {
        individualViewModal.getBookingSummary(bookingId)
    }
}