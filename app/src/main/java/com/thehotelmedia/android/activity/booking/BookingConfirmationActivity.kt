package com.thehotelmedia.android.activity.booking

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.razorpay.ExternalWalletListener
import com.razorpay.PaymentData
import com.razorpay.PaymentResultWithDataListener
import com.thehotelmedia.android.R
import com.thehotelmedia.android.ViewModelFactory
import com.thehotelmedia.android.activity.BaseActivity
import com.thehotelmedia.android.activity.userTypes.business.bottomNavigation.BottomNavigationBusinessMainActivity
import com.thehotelmedia.android.activity.userTypes.individual.bottomNavigation.BottomNavigationIndividualMainActivity
import com.thehotelmedia.android.adapters.booking.Guest
import com.thehotelmedia.android.adapters.booking.GuestDetailsAdapter
import com.thehotelmedia.android.customClasses.ColorFilterTransformation
import com.thehotelmedia.android.customClasses.Constants.business_type_individual
import com.thehotelmedia.android.customClasses.CustomProgressBar
import com.thehotelmedia.android.customClasses.CustomSnackBar
import com.thehotelmedia.android.customClasses.PaymentUtils
import com.thehotelmedia.android.customClasses.PreferenceManager
import com.thehotelmedia.android.customClasses.SuccessGiff
import com.thehotelmedia.android.databinding.ActivityBookingConfirmationBinding
import com.thehotelmedia.android.extensions.blurTheView
import com.thehotelmedia.android.extensions.formatBookingDates
import com.thehotelmedia.android.extensions.navigateToMainActivity
import com.thehotelmedia.android.extensions.roundToTwoDecimal
import com.thehotelmedia.android.extensions.setRatingWithStarWithoutBracket
import com.thehotelmedia.android.extensions.setRoomTypeImage
import com.thehotelmedia.android.modals.booking.checkout.Data
import com.thehotelmedia.android.repository.IndividualRepo
import com.thehotelmedia.android.viewModal.individualViewModal.IndividualViewModal
import org.json.JSONArray

class BookingConfirmationActivity : BaseActivity(), PaymentResultWithDataListener,
    ExternalWalletListener, DialogInterface.OnClickListener  {

    private lateinit var binding: ActivityBookingConfirmationBinding

    private lateinit var guestDetailsAdapter : GuestDetailsAdapter
    private var pricePerNight = 0.0
    private var maxGuestNumber = 1
    private var roomId = "1"
    private var bookingId = "1"
    private var roomCount = 1
    private var promoCodeApplied = false


    private lateinit var individualViewModal: IndividualViewModal
    private lateinit var progressBar: CustomProgressBar
    private lateinit var successGiff: SuccessGiff
    private lateinit var preferenceManager : PreferenceManager

    private var guestDetails: List<String> = listOf()

    private var businessType : String = ""
    private var razorPayAmount = ""
    private var razorPayCurrency = ""
    private var razorPayEntity = ""
    private var razorPayReceipt = ""
    private var razorPayOrderId = ""
    private var razorPayDescription = ""

    private var userEmail = ""
    private var userMobileNumber = ""
    private var selectedGuest = "myself"

    private lateinit var alertDialogBuilder: AlertDialog.Builder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBookingConfirmationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val individualRepo = IndividualRepo(this)
        individualViewModal = ViewModelProvider(this, ViewModelFactory(null, individualRepo, null))[IndividualViewModal::class.java]
        progressBar = CustomProgressBar(this)
        successGiff = SuccessGiff(this)
        preferenceManager = PreferenceManager.getInstance(this)
        businessType = preferenceManager.getString(PreferenceManager.Keys.BUSINESS_TYPE, "").toString()

        alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder.setTitle("Payment Result")
        alertDialogBuilder.setCancelable(true)
        alertDialogBuilder.setPositiveButton("Ok", this)

        initUi()

    }

    private fun initUi() {

        roomId = intent.getStringExtra("ROOM_ID") ?: ""
        bookingId = intent.getStringExtra("BOOKING_ID") ?: ""
        roomCount = intent.getIntExtra("ROOM_COUNT", 0)
        maxGuestNumber = intent.getIntExtra("GUEST_COUNT", 0)
        pricePerNight = intent.getDoubleExtra("PRICE_PER_NIGHT", 0.0)

        println("sadfjasdkhajsd  price $pricePerNight")

        getBookingCheckOutData("")

        blurTheView(binding.payNowBlurView)
        binding.normalInfoLayout.visibility = View.VISIBLE
        binding.changeInfoBtn.visibility = View.VISIBLE
        binding.guestDetailsRv.visibility = View.GONE
        binding.addBtn.visibility = View.GONE





        binding.backBtn.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.applyPromoCodeBtn.setOnClickListener {
            if (promoCodeApplied){
                getBookingCheckOutData("")
            }else{
                val promoCode = binding.promoCodeEt.text?.toString()?.trim()
                if (!promoCode.isNullOrEmpty()){
                    getBookingCheckOutData(promoCode)
                }
            }
        }


        binding.changeRoomBtn.setOnClickListener {
            val intent = Intent(this, PlanDetailsActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
        }

        binding.changeInfoBtn.setOnClickListener {
            binding.normalInfoLayout.visibility = View.GONE
            binding.changeInfoBtn.visibility = View.GONE
            binding.guestDetailsRv.visibility = View.VISIBLE
            binding.addBtn.visibility = View.VISIBLE
        }

        // Add Guest on Click
        binding.addBtn.setOnClickListener {
            guestDetailsAdapter.addGuest()
        }



        binding.payNowBtn.setOnClickListener {
            val validationMessage = guestDetailsAdapter.validateGuests()
            if (validationMessage == null) {


                val guestJson  = guestDetailsAdapter.getGuestsAsJson()

                // Gson instance
                val gson = Gson()

                // JSON ko List<Map<String, Any>> me convert karna
                val listType = object : TypeToken<List<Map<String, Any>>>() {}.type
                val guests: List<Map<String, Any>> = gson.fromJson(guestJson, listType)

                // Har object ko JSON string me convert karke List<String> me daalna
                guestDetails = guests.map { gson.toJson(it) }



                PaymentUtils.startPayment(
                    this,
                    amount = razorPayAmount,
                    currency = razorPayCurrency,
                    entity = razorPayEntity,
                    receipt = razorPayReceipt,
                    orderId = razorPayOrderId,
                    userNumber = userMobileNumber,
                    email = userEmail,
                    description = razorPayDescription,
                    tag = "Booking"
                )
            } else {
                Toast.makeText(this, validationMessage, Toast.LENGTH_SHORT).show()
            }
        }


        individualViewModal.bookingCheckOutResult.observe(this){result->

            println("asdjhasjd    $result")
            if (result.status == true){
                val data = result.data
                handelCheckOutData(data)
            }else{
                binding.promoCodeLayout.visibility = View.GONE
                promoCodeApplied = false
                binding.promoCodeEt.text?.clear()
                binding.applyPromoCodeBtn.setTextColor(ContextCompat.getColor(this, R.color.blue))
                binding.applyPromoCodeBtn.text = getString(R.string.apply)
                binding.promoCodeEt.isEnabled = true
                val msg = result.message.orEmpty()

                CustomSnackBar.showSnackBar(binding.root,msg)
                if (msg.contains("room isn't available for the dates you picked", ignoreCase = true)) {
                    onBackPressedDispatcher.onBackPressed()
                }

            }
        }

        individualViewModal.bookRoomResult.observe(this){result->
            if (result.status == true){

                result.message?.let { msg ->
                    runOnUiThread {
                        successGiff.show(msg) {
                            navigateToMainActivity(businessType == business_type_individual)
                        }
                    }
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

    private fun demo(guestsAsJsonArray: JSONArray) {

    }

    private fun getBookingCheckOutData(promoCode: String) {
        individualViewModal.bookingCheckOut(bookingId,roomId,roomCount,promoCode,pricePerNight)
    }

    private fun handelCheckOutData(data: Data?) {

        val checkInDate = data?.checkIn ?: ""
        val checkOutDate = data?.checkOut ?: ""

        val roomType = data?.room?.title ?: ""
        val bedType =  data?.room?.bedType ?: ""

        val payment = data?.payment

        val gstPrice = payment?.gst.roundToTwoDecimal()
        val gstPercentage = payment?.gstRate.roundToTwoDecimal()
        val subtotal = payment?.subtotal.roundToTwoDecimal()
        val convinceCharges = payment?.convinceCharges.roundToTwoDecimal()
        val discount = payment?.discount ?: 0.0
        val promoCode = payment?.promoCode
        val promoCodeName = payment?.promoCode?.name ?: ""
        val promoCodeDescription = payment?.promoCode?.description ?: ""


        if (discount != 0.0){
            binding.promoCodeLayout.visibility = View.VISIBLE
            promoCodeApplied = true
            binding.applyPromoCodeBtn.setTextColor(ContextCompat.getColor(this, R.color.red))
            binding.applyPromoCodeBtn.text = getString(R.string.remove)
            binding.promoCodeEt.isEnabled = false
        }else{
            binding.promoCodeLayout.visibility = View.GONE
            promoCodeApplied = false
            binding.promoCodeEt.text?.clear()
            binding.applyPromoCodeBtn.setTextColor(ContextCompat.getColor(this, R.color.blue))
            binding.applyPromoCodeBtn.text = getString(R.string.apply)
            binding.promoCodeEt.isEnabled = true
        }

        binding.promoCodeNameTv.text = promoCodeName
        binding.promoCodeTv.text = "-₹$discount"



        val total = payment?.total.roundToTwoDecimal()

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

        val userData = data?.user
        userEmail = userData?.email ?: ""
        val fullName = userData?.name ?: ""

        val dialCode = userData?.dialCode ?: ""
        val phoneNumber = userData?.phoneNumber ?: ""
        userMobileNumber = "$dialCode$phoneNumber"

        val roomQuantity = data?.bookedRoom?.quantity ?: 1
        val razorPayData = data?.razorPayOrder

        binding.nameTv.text = fullName
        binding.emailTv.text = userEmail
        binding.phoneNumberTv.text = userMobileNumber
        if (userEmail.isEmpty()){
            binding.emailLayout.visibility  = View.GONE
        }
        if (userMobileNumber.isEmpty()){
            binding.numberLayout.visibility  = View.GONE

            binding.normalInfoLayout.visibility = View.GONE
            binding.changeInfoBtn.visibility = View.GONE
            binding.guestDetailsRv.visibility = View.VISIBLE
            binding.addBtn.visibility = View.VISIBLE
        }


        // Static user (first guest)
        val staticGuest = Guest(
            title = "Mr",
            fullName = fullName,
            email = userEmail,
            mobileNumber = phoneNumber
        )

        // Initial Guest List with static user at index 0
        val guests = mutableListOf(staticGuest) // Static user + an empty guest

        // Set Adapter
        guestDetailsAdapter = GuestDetailsAdapter(this, guests,maxGuestNumber,::onSelectedGuestType)
        binding.guestDetailsRv.adapter = guestDetailsAdapter



        // Razorpay expects amount in the smallest currency unit (e.g. paise) as an integer.
        // Prefer server-provided order amount; fall back to total*100 if missing.
        val serverAmountPaise = razorPayData?.amount
            ?: razorPayData?.amountDue
            ?: ((payment?.total ?: 0.0) * 100.0).toLong()
        razorPayAmount = serverAmountPaise.toString()
        razorPayCurrency = razorPayData?.currency ?: ""
        razorPayEntity = razorPayData?.entity ?: ""
        razorPayReceipt = razorPayData?.receipt ?: ""
        razorPayOrderId = razorPayData?.id ?: ""
        razorPayDescription = razorPayData?.notes?.description ?: "Booking"

        println("Booking Razorpay -> orderId=$razorPayOrderId amountPaise=$razorPayAmount currency=$razorPayCurrency")




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

        binding.roomTypeIv.setRoomTypeImage(bedType)

        binding.roomTypeTv.text = roomType
        binding.billingRoomType.text = roomType

        val (formattedCheckIn, formattedCheckOut, nights) = formatBookingDates(checkInDate, checkOutDate)
        binding.noOfNightTv.text = "${nights}${getString(R.string.night)}"
        binding.checkInDateTv.text = formattedCheckIn
        binding.checkOutDateTv.text = formattedCheckOut

        binding.planChargesTv.text = "₹$subtotal"
        binding.convinceChargesPriceTv.text = "₹$convinceCharges"
        binding.gstPercentageTv.text = "${getString(R.string.gst)}($gstPercentage%)"
        binding.gstPriceTv.text = "₹$gstPrice"
        binding.totalPriceTv.text = "₹$total"


    }

    private fun onSelectedGuestType(selectedGuestType: String) {
        selectedGuest = selectedGuestType
    }

    override fun onPaymentSuccess(p0: String?, p1: PaymentData?) {
        try {

            val orderId = p1?.orderId.toString()
            val paymentId = p1?.paymentId.toString()
            val signature = p1?.signature.toString()

            bookRoom(paymentId,signature,orderId)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun bookRoom(paymentId: String, signature: String,orderId : String) {
        individualViewModal.bookRoom(bookingId,paymentId,signature,guestDetails,selectedGuest)
    }

//    override fun onPaymentError(p0: Int, p1: String?, p2: PaymentData?) {
//        try {
//            alertDialogBuilder.setMessage("Payment Failed : Payment Data: ${p2?.data}")
//            alertDialogBuilder.show()
//        } catch (e: Exception) {
//            e.printStackTrace()
//        }
//    }
override fun onPaymentError(p0: Int, p1: String?, p2: PaymentData?) {
    try {
        alertDialogBuilder.setMessage("Payment Failed : Payment Data: ${p2?.data}")
        val alertDialog = alertDialogBuilder.create()

        alertDialog.setOnShowListener {
            val positiveButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setTextColor(ContextCompat.getColor(this, R.color.blue))
        }

        alertDialog.show()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}


//    override fun onExternalWalletSelected(p0: String?, p1: PaymentData?) {
//        try {
//            alertDialogBuilder.setMessage("External wallet was selected : Payment Data: ${p1?.data}")
//            alertDialogBuilder.show()
//        } catch (e: Exception) {
//            e.printStackTrace()
//        }
//    }
override fun onExternalWalletSelected(p0: String?, p1: PaymentData?) {
    try {
        alertDialogBuilder.setMessage("External wallet was selected : Payment Data: ${p1?.data}")
        val alertDialog = alertDialogBuilder.create()

        alertDialog.setOnShowListener {
            val positiveButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setTextColor(ContextCompat.getColor(this, R.color.blue))
        }

        alertDialog.show()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}


    override fun onClick(dialog: DialogInterface?, which: Int) {
        dialog?.dismiss()
    }

}