package com.thehotelmedia.android.activity.authentication.business

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.razorpay.ExternalWalletListener
import com.razorpay.PaymentData
import com.razorpay.PaymentResultWithDataListener
import com.thehotelmedia.android.R
import com.thehotelmedia.android.ViewModelFactory
import com.thehotelmedia.android.activity.BaseActivity
import com.thehotelmedia.android.activity.authentication.SignInActivity
import com.thehotelmedia.android.activity.userTypes.business.bottomNavigation.BottomNavigationBusinessMainActivity
import com.thehotelmedia.android.activity.userTypes.individual.bottomNavigation.BottomNavigationIndividualMainActivity
import com.thehotelmedia.android.customClasses.ColorFilterTransformation
import com.thehotelmedia.android.customClasses.Constants.business_type_individual
import com.thehotelmedia.android.customClasses.CustomProgressBar
import com.thehotelmedia.android.customClasses.CustomSnackBar
import com.thehotelmedia.android.customClasses.DocumentVerificationGiff
import com.thehotelmedia.android.customClasses.PreferenceManager
import com.thehotelmedia.android.customClasses.billing.BillingManager
import com.thehotelmedia.android.databinding.ActivityReviewSummaryBinding
import com.thehotelmedia.android.extensions.toTimePeriod
import com.thehotelmedia.android.modals.authentication.business.subscriptionCheckOut.Data
import com.thehotelmedia.android.repository.AuthRepo
import com.thehotelmedia.android.viewModal.authViewModel.AuthViewModel

class ReviewSummaryActivity : BaseActivity(), PaymentResultWithDataListener, ExternalWalletListener, DialogInterface.OnClickListener {

    private lateinit var binding: ActivityReviewSummaryBinding
    private var isPromoCodeAdded = false

    private lateinit var authViewModel: AuthViewModel
    private val activity = this@ReviewSummaryActivity
    private var subscriptionPlanId: String = ""
    private var from: String = ""
    private var promoCode: String = ""

    private lateinit var progressBar: CustomProgressBar

    private var userDialCode: String = ""
    private var userPhoneNumber: String = ""
    private var normalOrderId: String = ""
    private var razorPayOrderId: String = ""
    private var razorPayAmount: String = ""
    private var razorPayCurrency: String = ""
    private var razorPayEntity: String = ""
    private var razorPayReceipt: String = ""
    private var googleSubscriptionId: String = ""

    private var businessType : String = ""
    private lateinit var preferenceManager : PreferenceManager

    private lateinit var documentVerificationGiff: DocumentVerificationGiff

    private lateinit var alertDialogBuilder: AlertDialog.Builder

    private lateinit var billingManager: BillingManager


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityReviewSummaryBinding.inflate(layoutInflater)

        setContentView(binding.root)

        billingManager = BillingManager(this) { success, message,purchaseDetails ->
            if (success){
                val purchaseToken = purchaseDetails?.purchaseToken ?: ""
                subscriptionVerifyByGoogle(purchaseToken,googleSubscriptionId,normalOrderId)
            }
        }


        initUI()
    }

    private fun initUI() {
        binding.hasDataLayout.visibility = View.GONE

        documentVerificationGiff = DocumentVerificationGiff(activity) // 'this' refers to the context
        subscriptionPlanId = intent.getStringExtra("SUBSCRIPTION_ID") ?: ""

        from = intent.getStringExtra("FROM") ?: ""
        val authRepo = AuthRepo(activity)
        authViewModel = ViewModelProvider(activity, ViewModelFactory(authRepo))[AuthViewModel::class.java]
        progressBar = CustomProgressBar(activity) // 'this' refers to the context
        preferenceManager = PreferenceManager.getInstance(activity)
        businessType = preferenceManager.getString(PreferenceManager.Keys.BUSINESS_TYPE, "").toString()
        getCheckOutData()

        alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder.setTitle("Payment Result")
        alertDialogBuilder.setCancelable(true)
        alertDialogBuilder.setPositiveButton("Ok", this)


        binding.promoCodeEt.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                // When EditText is focused
                binding.promoCodeEtLayout.setBackgroundResource(R.drawable.rounded_edit_text_background_focused)
            } else {
                // When EditText loses focus
                binding.promoCodeEtLayout.setBackgroundResource(R.drawable.rounded_edit_text_background_normal)
            }
        }

        binding.backBtn.setOnClickListener {
            this.onBackPressedDispatcher.onBackPressed()
        }

//        binding.nextBtn.setOnClickListener {
//
//            if (googleSubscriptionId.isNotEmpty()){
//                billingManager.purchaseSubscription(googleSubscriptionId)
//            }else{
//                CustomSnackBar.showSnackBar(binding.root,"Unable to fetch subscription plan")
//            }
//
////            PaymentUtils.startPayment(
////                this,
////                amount = razorPayAmount,
////                currency = razorPayCurrency,
////                entity = razorPayEntity,
////                receipt = razorPayReceipt,
////                orderId = razorPayOrderId,
////                userNumber = "$userDialCode$userPhoneNumber",
////                email = "----------",
////                description = "Subscription",
////                tag = "Subscription"
////            )
//
//        }

        binding.nextBtn.setOnClickListener {
            binding.nextBtn.isEnabled = false
            if (googleSubscriptionId.isNotEmpty()) {
                billingManager.purchaseSubscription(googleSubscriptionId){
                    binding.nextBtn.isEnabled = true
                }
            } else {
                binding.nextBtn.isEnabled = true
                CustomSnackBar.showSnackBar(binding.root, "Unable to fetch subscription plan")
            }
        }


//        binding.addPromoCodeBtn.setOnClickListener {
//            if (!isPromoCodeAdded) {
//                promoCode = binding.promoCodeEt.text.toString().trim()
//                if (promoCode.isEmpty()){
//                    return@setOnClickListener
//                }
//                binding.addPromoCodeBtn.animate().rotationBy(45f).setDuration(200).start()
//                isPromoCodeAdded = true
//                binding.promoCodeEt.isEnabled = false
//                getCheckOutData()
//            } else {
//                binding.addPromoCodeBtn.animate().rotationBy(-45f).setDuration(200).start()
//                isPromoCodeAdded = false
//                promoCode = ""
//                binding.promoCodeEt.text?.clear()
//                binding.promoCodeEt.isEnabled = true
//                getCheckOutData()
//            }
//        }
        binding.applyPromoCodeBtn.setOnClickListener {
            if (!isPromoCodeAdded) {
                promoCode = binding.promoCodeEt.text.toString().trim()
                if (promoCode.isEmpty()){
                    return@setOnClickListener
                }
                binding.applyPromoCodeBtn.apply {
                    text = getString(R.string.remove)
                    setTextColor(ContextCompat.getColor(context, R.color.grey))
                }
                isPromoCodeAdded = true
                binding.promoCodeEt.isEnabled = false
                getCheckOutData()
            } else {
                binding.applyPromoCodeBtn.apply {
                    text = getString(R.string.apply)
                    setTextColor(ContextCompat.getColor(context, R.color.blue))
                }
                isPromoCodeAdded = false
                promoCode = ""
                binding.promoCodeEt.text?.clear()
                binding.promoCodeEt.isEnabled = true
                getCheckOutData()
            }
        }

        authViewModel.subscriptionCheckOutResult.observe(activity){result->
            if (result.status==true){
                binding.hasDataLayout.visibility = View.VISIBLE
                handelCheckOutData(result.data)
            }else{

                val msg = result.message
                val statusCode = result.statusCode
                if (statusCode == 404){
                    if (!isPromoCodeAdded) {

                        binding.applyPromoCodeBtn.apply {
                            text = getString(R.string.remove)
                            setTextColor(ContextCompat.getColor(context, R.color.grey))
                        }
                        promoCode = binding.promoCodeEt.text.toString().trim()
                        isPromoCodeAdded = true
                        binding.promoCodeEt.isEnabled = false
                    } else {

                        binding.applyPromoCodeBtn.apply {
                            text = getString(R.string.apply)
                            setTextColor(ContextCompat.getColor(context, R.color.blue))
                        }
                        isPromoCodeAdded = false
                        promoCode = ""
                        binding.promoCodeEt.text?.clear()
                        binding.promoCodeEt.isEnabled = true
                    }
                }
                Toast.makeText(activity,msg, Toast.LENGTH_SHORT).show()
            }
        }

        authViewModel.buySubscriptionResult.observe(activity){result->
            if (result.status==true){

                if (from == "SettingsSubscription"){

                    if (businessType == business_type_individual){
                        val intent = Intent(activity, BottomNavigationIndividualMainActivity::class.java)
                        startActivity(intent)
                        finish()
                    }else {
                        val intent = Intent(activity, BottomNavigationBusinessMainActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                    val msg = result.message
                    Toast.makeText(activity,msg, Toast.LENGTH_SHORT).show()

                } else{
                    val msg = result.message ?: ""
                    documentVerificationGiff.show(msg) {
                        val intent = Intent(this, SignInActivity::class.java)
                        startActivity(intent)
                        finish()
                        // Callback when animation is complete
                        // Navigate to another activity or perform any action here
                    }
                }

            }else{
                val msg = result.message
                Toast.makeText(activity,msg, Toast.LENGTH_SHORT).show()
            }
        }



        authViewModel.subscriptionVerifyByGoogleResult.observe(activity){result->
            if (result.status==true){

                if (from == "SettingsSubscription"){

                    if (businessType == business_type_individual){
                        val intent = Intent(activity, BottomNavigationIndividualMainActivity::class.java)
                        startActivity(intent)
                        finish()
                    }else {
                        val intent = Intent(activity, BottomNavigationBusinessMainActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                    val msg = result.message
                    Toast.makeText(activity,msg, Toast.LENGTH_SHORT).show()

                } else{
                    val msg = result.message ?: ""
                    documentVerificationGiff.show(msg) {
                        val intent = Intent(this, SignInActivity::class.java)
                        startActivity(intent)
                        finish()
                        // Callback when animation is complete
                        // Navigate to another activity or perform any action here
                    }
                }

            }else{
                val msg = result.message
                Toast.makeText(activity,msg, Toast.LENGTH_SHORT).show()
            }
        }


        authViewModel.loading.observe(activity){
            if (it == true){
                progressBar.show() // To show the progress bar
            }else{
                progressBar.hide() // To hide the progress bar
            }
        }

        authViewModel.toast.observe(activity){
            Toast.makeText(activity,it, Toast.LENGTH_SHORT).show()
        }

    }



    private fun getCheckOutData() {
        authViewModel.subscriptionCheckOut(subscriptionPlanId,promoCode)
    }

//    private fun normalPromoCodeBtn() {
//        if (isPromoCodeAdded) {
//            binding.addPromoCodeBtn.animate().rotationBy(-45f).setDuration(200).start()
//            isPromoCodeAdded = false
//
//        }
//    }
    private fun buySubscription(paymentId: String, signature: String) {
        authViewModel.buySubscription(normalOrderId,paymentId,signature)
    }
    private fun subscriptionVerifyByGoogle(purchaseToken: String,subscriptionId: String,orderId: String) {
        println("safjlskdahkas   subscriptionId $subscriptionId")
        println("safjlskdahkas   orderId $orderId")
        println("safjlskdahkas   purchaseToken $purchaseToken")
        authViewModel.subscriptionVerifyByGoogle(purchaseToken,subscriptionId,orderId)
    }

    private fun handelCheckOutData(data: Data?) {


        val razorPayOrder = data?.razorPayOrder

        normalOrderId = data?.orderID.toString()
        razorPayOrderId = razorPayOrder?.id.toString()
        razorPayAmount = razorPayOrder?.amount.toString()
        razorPayCurrency = razorPayOrder?.currency.toString()
        razorPayEntity = razorPayOrder?.entity.toString()
        razorPayReceipt = razorPayOrder?.receipt.toString()



        val billingAddress = data?.billingAddress
        val userName = billingAddress?.name
        userDialCode = billingAddress?.dialCode.toString()
        userPhoneNumber = billingAddress?.phoneNumber.toString()
        val gstNumber = billingAddress?.gstn.toString()
        val userAddress = billingAddress?.address
        val userStreet = userAddress?.street
        val userCity = userAddress?.city
        val userState = userAddress?.state
        val userZipCode = userAddress?.zipCode
        val userCountry = userAddress?.country
        val address = "$userStreet, $userCity, $userState, $userZipCode."

        val plan = data?.plan
        val planName = plan?.name
        val planPrice = plan?.price
        val planImage = plan?.image
        val planDuration = plan?.duration
        googleSubscriptionId = plan?.googleSubscriptionID.toString().trim() ?: ""

        val timePeriod = planDuration?.toTimePeriod()

        val payment = data?.payment
        val subtotal = payment?.subtotal
        val gst = payment?.gst
        val gstPercentage = payment?.gstRate?.toInt()
        val total = payment?.total
        val discount = payment?.discount


//        val code = payment?.promoCode?.code ?: ""
//        val priceType = payment?.promoCode?.priceType ?: ""
//        var value = ""

        val code = payment?.promoCode?.code ?: ""
        val priceType = payment?.promoCode?.priceType ?: ""
        var value = ""
        val discountText = if (discount != null) "-₹$discount" else ""

        if (code.isNotEmpty()) {
            binding.promoCodeLayout.visibility = View.VISIBLE

            value = if (priceType == "percent") {
                "-${payment?.promoCode?.value}%"
            } else {
                "-${payment?.promoCode?.value}"
            }

            // Create a SpannableString to style different parts of the text
            val promoText = "$value ($code) $discountText"
            val spannableString = SpannableString(promoText)

            // Set color for `value`
            spannableString.setSpan(
                ForegroundColorSpan(Color.RED), 0, value.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            // Set color for `code`
            spannableString.setSpan(
                ForegroundColorSpan(Color.WHITE), value.length + 1, value.length + 1 + code.length + 2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            // Set color for `discountText` (including ₹ symbol)
            if (discountText.isNotEmpty()) {
                spannableString.setSpan(
                    ForegroundColorSpan(Color.RED),
                    promoText.length - discountText.length,
                    promoText.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }

            // Apply the SpannableString to the TextView
            binding.promoCodeTv.text = spannableString

        } else {
            binding.promoCodeLayout.visibility = View.GONE
        }






//        if (code.isNotEmpty()) {
//            binding.promoCodeLayout.visibility = View.VISIBLE
//
//            if (priceType == "percent") {
//                value = "-${payment?.promoCode?.value}%"
//            } else {
//                value = "-${payment?.promoCode?.value}"
//            }
//
//            // Create a SpannableString to style different parts of the text
//            val promoText = "$value ($code)"
//            val spannableString = SpannableString(promoText)
//
//            // Set color for `value`
//            spannableString.setSpan(
//                ForegroundColorSpan(Color.RED), 0, value.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
//            )
//
//            // Set color for `code`
//            spannableString.setSpan(
//                ForegroundColorSpan(Color.WHITE), value.length + 1, promoText.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
//            )
//
//            // Apply the SpannableString to the TextView
//            binding.promoCodeTv.text = spannableString
//
//        } else {
//            binding.promoCodeLayout.visibility = View.GONE
//        }


        binding.nameTv.text = userName
        binding.addressTv.text = address
        binding.phoneTv.text = "$userDialCode$userPhoneNumber"
        binding.gstnTv.text = gstNumber

        binding.planTv.text = planName
        binding.planTime.text = timePeriod
        binding.planChargesTv1.text = "INR $planPrice"
        Glide.with(activity).load(planImage)
            .placeholder(R.drawable.ic_standard)
            .transform(ColorFilterTransformation(ContextCompat.getColor(this, R.color.icon_color_80)))
            .into(binding.planIcon)


        if (gstNumber.isEmpty()){
            binding.gstnLayout.visibility = View.GONE
        }
        if (userPhoneNumber.isEmpty()){
            binding.phoneLayout.visibility = View.GONE
        }

        binding.gstPercentageTv.text = "${getString(R.string.gst)}($gstPercentage%)"
//        binding.planChargesTv.text = "₹$subtotal"
        binding.planChargesTv.text = "INR $total"
        binding.gstPriceTv.text = "INR $gst"
        binding.totalPriceTv.text = "INR $total"

        if (gst == 0.0){
            binding.gstPriceLayout.visibility = View.GONE
        }

    }

    override fun onPaymentSuccess(p0: String?, p1: PaymentData?) {
        try {

            val orderId = p1?.orderId.toString()
            val paymentId = p1?.paymentId.toString()
            val signature = p1?.signature.toString()

            buySubscription(paymentId,signature)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onPaymentError(p0: Int, p1: String?, p2: PaymentData?) {
        try {
            println("asdfjlksadjfklaj p0 $p0")
            println("asdfjlksadjfklaj p1 $p1")
            println("asdfjlksadjfklaj p2 ${p2?.data}")
            alertDialogBuilder.setMessage("Payment Failed : Payment Data: ${p2?.data}")
            alertDialogBuilder.show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onExternalWalletSelected(p0: String?, p1: PaymentData?) {
        try {
            alertDialogBuilder.setMessage("External wallet was selected : Payment Data: ${p1?.data}")
            alertDialogBuilder.show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onClick(dialog: DialogInterface?, which: Int) {
        dialog?.dismiss()
    }

}