package com.thehotelmedia.android.activity.authentication

import android.Manifest
import android.animation.Animator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.util.Log
import android.util.Patterns
import android.view.MotionEvent
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.google.gson.Gson
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.messaging.FirebaseMessaging
import com.thehotelmedia.android.R
import com.thehotelmedia.android.ViewModelFactory
import com.thehotelmedia.android.activity.BaseActivity
import com.thehotelmedia.android.activity.authentication.business.BusinessQuestionsActivity
import com.thehotelmedia.android.activity.authentication.business.BusinessTypeMediaActivity
import com.thehotelmedia.android.activity.authentication.business.BusinessVerifyEmailActivity
import com.thehotelmedia.android.activity.authentication.business.BusinessSubscriptionActivity
import com.thehotelmedia.android.activity.authentication.business.SupportingDocumentsActivity
import com.thehotelmedia.android.activity.authentication.forgetPassword.ResetPasswordActivity
import com.thehotelmedia.android.activity.authentication.individual.IndividualMediaActivity
import com.thehotelmedia.android.activity.authentication.individual.IndividualVerifyEmailActivity
import com.thehotelmedia.android.activity.authentication.individual.ValidationResult
import com.thehotelmedia.android.activity.userTypes.business.bottomNavigation.BottomNavigationBusinessMainActivity
import com.thehotelmedia.android.activity.userTypes.individual.bottomNavigation.BottomNavigationIndividualMainActivity
import com.thehotelmedia.android.activity.userTypes.individual.settingsScreen.HelpAndSupportActivity
import com.thehotelmedia.android.customClasses.Constants.DEFAULT_LAT
import com.thehotelmedia.android.customClasses.Constants.DEFAULT_LNG
import com.thehotelmedia.android.customClasses.Constants.LANGUAGE_CODE
import com.thehotelmedia.android.customClasses.Constants.N_A
import com.thehotelmedia.android.customClasses.Constants.business_type_individual
import com.thehotelmedia.android.customClasses.CustomProgressBar
import com.thehotelmedia.android.customClasses.CustomSnackBar
import com.thehotelmedia.android.customClasses.DocumentVerificationGiff
import com.thehotelmedia.android.customClasses.MessageStore
import com.thehotelmedia.android.customClasses.PreferenceManager
import com.thehotelmedia.android.customDialog.ProfessionDialog
import com.thehotelmedia.android.databinding.ActivitySignInBinding
import com.thehotelmedia.android.extensions.LocationHelper
import com.thehotelmedia.android.extensions.capitalizeFirstLetter
import com.thehotelmedia.android.extensions.getAndroidDeviceId
import com.thehotelmedia.android.extensions.setEmailTextWatcher
import com.thehotelmedia.android.modals.authentication.login.LoginModal
import com.thehotelmedia.android.repository.AuthRepo
import com.thehotelmedia.android.repository.IndividualRepo
import com.thehotelmedia.android.viewModal.authViewModel.AuthViewModel
import com.thehotelmedia.android.viewModal.individualViewModal.IndividualViewModal


class SignInActivity : BaseActivity() {


    private var retryCount = 0
    private val maxRetries = 3

    private lateinit var locationHelper: LocationHelper
    private var userLat = 0.0
    private var userLng = 0.0
    // Define the permission launcher as a class property
    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
                // Permissions granted, proceed to fetch location
                locationHelper.checkAndRequestLocation()
            } else {
                userLat = DEFAULT_LAT
                userLng = DEFAULT_LNG
                getWeatherAndAQI(userLat,userLng)
            }
        }

    private lateinit var individualViewModal: IndividualViewModal
    private lateinit var binding: ActivitySignInBinding
    private var isPasswordVisible = false
    private lateinit var authViewModel: AuthViewModel
    private val activity = this@SignInActivity
    private var deviceId = ""
    private var fcmToken = N_A
    private var currentLanguage = ""
    private var TAG = "GOOGLE_SIGN_IN"
    private lateinit var preferenceManager : PreferenceManager
    private lateinit var  progressBar : CustomProgressBar
    private lateinit var documentVerificationGiff: DocumentVerificationGiff
    private lateinit var googleSignInClient: GoogleSignInClient
    // ActivityResultLauncher for handling sign-in result
    private val signInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleSignInResult(task)
        } else {
            CustomSnackBar.showSnackBar(binding.root, MessageStore.somethingWentWrong(this))
            result.data?.extras?.let { bundle ->
                for (key in bundle.keySet()) {
                    Log.d(TAG, "Extra: $key = ${bundle[key]}")
                }
            }
        }
    }

    private val phoneSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { activityResult ->
        if (activityResult.resultCode == Activity.RESULT_OK) {
            val data = activityResult.data
            val loginResultJson = data?.getStringExtra(PhoneSignInActivity.EXTRA_LOGIN_RESULT)
            if (!loginResultJson.isNullOrEmpty()) {
                val loginModal = Gson().fromJson(loginResultJson, LoginModal::class.java)
                handleLoginResult(loginModal)
            } else {
                CustomSnackBar.showSnackBar(binding.root, "Unable to login via phone. Please try again.")
            }
        }
    }



    // Method to handle the result of the sign-in intent
    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            // If the sign-in is successful, get the GoogleSignInAccount object
            val account = completedTask.getResult(ApiException::class.java)
            // Update the UI with the signed-in account's information
            updateGoogleInfo(account)
        } catch (e: ApiException) {
            // If the sign-in fails, log the error and update the UI accordingly
            CustomSnackBar.showSnackBar(binding.root,MessageStore.somethingWentWrong(this))
            updateGoogleInfo(null)
        }
    }

    // Method to update the UI based on the GoogleSignInAccount object
    private fun updateGoogleInfo(account: GoogleSignInAccount?) {
        if (account != null) {
            // If the account is not null, the sign-in was successful
            progressBar.show()

            // Retrieve and use the account information
            val personIdToken = account.idToken.toString()
            val personName = account.givenName.toString()
            val personFamilyName = account.familyName.toString()
            val personEmail = account.email.toString()
            val socialId = account.id.toString()
            val personPhoto = account.photoUrl
            var image = ""
            image = if (personPhoto == null){
                ""
            }else{
                personPhoto.toString()
            }

            val username = "$personName $personFamilyName"
            println("askjfsakljflk  personIdToken -> $personIdToken")
            println("askjfsakljflk  personName -> $personName")
            println("askjfsakljflk  personFamilyName -> $personFamilyName")
            println("askjfsakljflk  personEmail -> $personEmail")
            println("askjfsakljflk  personId -> $socialId")
            println("askjfsakljflk  personPhoto -> $personPhoto")

            socialLogin(personIdToken,"google")

            progressBar.hide()
            googleSignOut()
        } else {
            // If the account is null, the sign-in failed
            progressBar.hide()
            CustomSnackBar.showSnackBar(binding.root,MessageStore.googleLoginFailed(this))
        }
    }


    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            println("Notification permission  granted")
        } else {
            println("Notification permission  denied")
        }

        checkLocationPermission()

    }

    private fun socialLogin(personIdToken: String, socialType: String) {
        authViewModel.socialLogin(socialType, personIdToken,"","",deviceId,fcmToken,userLat,userLng,currentLanguage)
    }


    // Method to initiate the Google Sign-In process
    private fun googleSignIn() {
        val signInIntent = googleSignInClient.signInIntent
        signInLauncher.launch(signInIntent)
    }

    private fun googleSignOut() {
        googleSignInClient.signOut().addOnCompleteListener(this) {
            // Update UI after sign-out
//            updateGoogleInfo(null)
//            Toast.makeText(this, "Signed out successfully", Toast.LENGTH_SHORT).show()
        }
    }
    private fun checkLocationPermission() {
        // Initialize the LocationHelper with required callbacks
        locationHelper = LocationHelper(
            context = this,
            permissionLauncher = permissionLauncher,
            locationCallback = { latitude, longitude ->
                userLat = latitude
                userLng = longitude
                getWeatherAndAQI(latitude,longitude)
            },
            errorCallback = { errorMessage ->
                // Handle error callback
                Toast.makeText(this, "Error: $errorMessage", Toast.LENGTH_SHORT).show()
                finish()
            }
        )

        // Now check and request location permission when needed
        locationHelper.checkAndRequestLocation()
    }

    private fun getWeatherAndAQI(latitude: Double, longitude: Double) {

        println("adfasdklask    $latitude , $longitude")
        individualViewModal.getWeather(latitude,longitude)
        individualViewModal.getAQI(latitude,longitude)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySignInBinding.inflate(layoutInflater)

        setIconAnimation()
        setContentView(binding.root)

        checkAndRequestNotificationPermission()

        initUi()

    }

    private fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Permission already granted
                    println("Notification permission already granted")
                }

                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    // Show an explanation to the user

                    println("Notification permission is required to send notifications")
                }

                else -> {
                    // Directly request the permission
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            // Notification permission not needed below Android 13
            Toast.makeText(this, "Notification permission not required on this device", Toast.LENGTH_SHORT).show()
        }
        checkLocationPermission()
    }

//    private fun getFcmToken() {
//        FirebaseMessaging.getInstance().token
//            .addOnCompleteListener { task ->
//                if (!task.isSuccessful) {
//                    Log.w("FCM_TOEKN", "Fetching FCM registration token failed", task.exception)
//                    return@addOnCompleteListener
//                }
//                // Get the token
//                fcmToken = task.result
//                Log.d("FCM_TOEKN", "FCM Registration Token: $fcmToken")
//                // Now you can send this token to your server or use it as needed
//            }
//    }

    private fun getFcmTokenWithRetry() {
        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Success: Token mil gaya
                    val token = task.result
                    Log.d("FCM_TOKEN", "FCM Registration Token: $token")
                    fcmToken = token
                    retryCount = 0 // Reset retry count
                    return@addOnCompleteListener
                }

                // Fail: Retry if limit not reached
                Log.w("FCM_TOKEN", "Attempt ${retryCount + 1} failed", task.exception)
                retryCount++
                if (retryCount < maxRetries) {
                    getFcmTokenWithRetry() // Try again
                } else {
                    Log.e("FCM_TOKEN", "Failed after $maxRetries attempts")
                }
            }
    }



    private fun initUi() {

        val individualRepo = IndividualRepo(this)
        individualViewModal = ViewModelProvider(this, ViewModelFactory(null, individualRepo, null))[IndividualViewModal::class.java]

        val onBoarding = getSharedPreferences("ONBOARD", MODE_PRIVATE)
        currentLanguage = onBoarding.getString(LANGUAGE_CODE, "en").orEmpty()

//        getFcmToken()
        getFcmTokenWithRetry()
        val authRepo = AuthRepo(activity)
        authViewModel = ViewModelProvider(activity, ViewModelFactory(authRepo))[AuthViewModel::class.java]
        preferenceManager = PreferenceManager.getInstance(activity)
        progressBar = CustomProgressBar(activity) // 'this' refers to the context
        documentVerificationGiff = DocumentVerificationGiff(activity) // 'this' refers to the context
        deviceId = activity.getAndroidDeviceId()

        binding.emailEt.setEmailTextWatcher()

        // Configure sign-in options to request the user's ID, email address, and basic profile
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail() // Request the user's email
            .requestIdToken(getString(R.string.default_web_client_id)) // Replace with your server's client ID
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        setPasswordEt()
//        setToggleSwitch()

        binding.signupLayout.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }


        binding.helpSupportBtn.setOnClickListener {
            val intent = Intent(this, HelpAndSupportActivity::class.java)
            startActivity(intent)
        }

        binding.phoneBtn.setOnClickListener {
            val intent = Intent(this, PhoneSignInActivity::class.java).apply {
                putExtra(PhoneSignInActivity.EXTRA_DEVICE_ID, deviceId)
                putExtra(PhoneSignInActivity.EXTRA_NOTIFICATION_TOKEN, fcmToken)
                putExtra(PhoneSignInActivity.EXTRA_LAT, userLat)
                putExtra(PhoneSignInActivity.EXTRA_LNG, userLng)
                putExtra(PhoneSignInActivity.EXTRA_LANGUAGE, currentLanguage)
            }
            phoneSignInLauncher.launch(intent)
        }

        binding.forgetPassword.setOnClickListener {
            val intent = Intent(this, ResetPasswordActivity::class.java)
            startActivity(intent)
        }

        binding.googleBtn.setOnClickListener {
            googleSignIn()
        }
        binding.btnNext.setOnClickListener {
            val result = validateFields(binding.emailEt, binding.passwordEt)
            if (result.isValid) {
                login()
//                val intent = Intent(this, BottomNavigationIndividualMainActivity::class.java)
//                startActivity(intent)
                // Proceed with form submission
            } else {
                CustomSnackBar.showSnackBar(binding.root, result.errorMessage.toString())
            }
        }

        authViewModel.loginResult.observe(activity){result->
            if (result.status==true){
                handleLoginResult(result)
            }else{
                handelLoginErrorData(result)
            }
        }
        authViewModel.socialLoginResult.observe(activity){result->
            if (result.status==true){
                handleLoginResult(result)
            }else{
                handelLoginErrorData(result)
            }
        }

        individualViewModal.getWeatherResult.observe(this){result->
            if (result != null){
                val weatherTemp = result.main.feels_like ?: 0.0
                val weatherType = result.weather[0].main ?: ""


                preferenceManager.putDouble(PreferenceManager.Keys.WEATHER_TEMP,weatherTemp)
                preferenceManager.putString(PreferenceManager.Keys.WEATHER_TYPE,weatherType.lowercase())
            }
        }
        individualViewModal.getAQIResult.observe(this){result->
            if (result != null){
                val aqi = result.list[0].main?.aqi ?: 0
                val pm25 = result.list[0].components?.pm25 ?: 0.0
                preferenceManager.putInt(PreferenceManager.Keys.AQI, aqi)
                preferenceManager.putDouble(PreferenceManager.Keys.AQI_PM25, pm25)
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
//            Toast.makeText(activity,it, Toast.LENGTH_SHORT).show()
//            CustomSnackBar.showSnackBar(binding.root,it)
        }

    }

    private fun handelLoginErrorData(result: LoginModal?) {
        val statusCode = result?.statusCode
        val isVerified = result?.data?.isVerified
        val hasSubscription = result?.data?.hasSubscription

        if (statusCode == 403 && isVerified == false || statusCode == 403 && hasSubscription == false){
            handleLoginResult(result)
        }else{
            val msg = result?.message.toString()
            val isApproved = result?.data?.isApproved ?: true
            val isActivated = result?.data?.isActivated ?: false
            val isDeleted = result?.data?.isDeleted ?: false

            if (!isApproved){
                    documentVerificationGiff.show(msg) {
                        // Callback when animation is complete
                        // Navigate to another activity or perform any action here
                    }
            }else{
                if (msg.contains("deleted") || msg.contains("inactive")) {
                    documentVerificationGiff.show(msg) {
                        // Callback when animation is complete
                        // Navigate to another activity or perform any action here
                    }
                } else {
                    CustomSnackBar.showSnackBar(binding.root,msg)
                }
            }

        }

    }


    private fun handleLoginResult(result: LoginModal?) {


        val accountType = result?.data?.accountType?.capitalizeFirstLetter() ?: ""
        val userId = result?.data?.id ?: ""
        val accessToken = result?.data?.accessToken ?: ""
        val refreshToken = result?.data?.refreshToken ?: ""

        val profession = result?.data?.profession ?: ""

        var fullName = ""
        val username = result?.data?.username ?: ""
        val email = result?.data?.email ?: ""
        val dialCode = result?.data?.dialCode ?: ""
        val phoneNumber = result?.data?.phoneNumber ?: ""
        val isVerified = result?.data?.isVerified ?: false
        val isApproved = result?.data?.isApproved ?: false
        val hasProfilePicture = result?.data?.hasProfilePicture ?: false
        val acceptedTerms = result?.data?.acceptedTerms ?: false
        val hasAmenities = result?.data?.hasAmenities ?: false
        val isDocumentUploaded = result?.data?.isDocumentUploaded ?: false
        val hasSubscription = result?.data?.hasSubscription ?: false
        var userSmallProfilePic = ""
        var userMediumProfilePic = ""
        var userLargeProfilePic = ""
        println("adsfkakjhdf    hasProfilePicture : $hasProfilePicture \n  userSmallProfilePic : $userSmallProfilePic")


        val cookies = "SessionToken=$refreshToken; UserDeviceID=$deviceId; X-Access-Token= $accessToken"

        // Save all critical authentication data atomically using batch method
        // This ensures all auth data is saved together in a single commit operation
        preferenceManager.saveAuthDataBatch(
            accessToken = accessToken,
            refreshToken = refreshToken,
            userId = userId,
            cookies = cookies,
            businessType = accountType,
            userAcceptedTerms = acceptedTerms
        )
        
        // Verify critical data was saved immediately after batch save
        val savedToken = preferenceManager.getString(PreferenceManager.Keys.ACCESS_TOKEN, "")
        val savedUserId = preferenceManager.getString(PreferenceManager.Keys.USER_ID, "")
        if (savedToken.isNullOrEmpty() || savedUserId.isNullOrEmpty()) {
            android.util.Log.e("SignInActivity", "CRITICAL: Failed to save auth data! Token: ${!savedToken.isNullOrEmpty()}, UserId: ${!savedUserId.isNullOrEmpty()}")
        } else {
            android.util.Log.d("SignInActivity", "Successfully saved auth data - Token length: ${savedToken?.length ?: 0}, UserId: $savedUserId")
        }

        preferenceManager.putString(PreferenceManager.Keys.USER_USER_NAME, username)
        preferenceManager.putString(PreferenceManager.Keys.USER_EMAIL, email)
        preferenceManager.putString(PreferenceManager.Keys.USER_DIAL_CODE, dialCode)
        preferenceManager.putString(PreferenceManager.Keys.USER_PHONE_NUMBER, phoneNumber)



        if (accountType == business_type_individual){
            fullName = result?.data?.name ?: ""
            userSmallProfilePic = result?.data?.profilePic?.small ?: ""
            userMediumProfilePic = result?.data?.profilePic?.medium ?: ""
            userLargeProfilePic = result?.data?.profilePic?.large ?: ""
            preferenceManager.putString(PreferenceManager.Keys.USER_FULL_NAME, fullName)
            preferenceManager.putString(PreferenceManager.Keys.USER_SMALL_PROFILE_PIC, userSmallProfilePic)
            preferenceManager.putString(PreferenceManager.Keys.USER_MEDIUM_PROFILE_PIC, userMediumProfilePic)
            preferenceManager.putString(PreferenceManager.Keys.USER_LARGE_PROFILE_PIC, userLargeProfilePic)
//            if (!isVerified){
//                moveToIndividualVerifyEmailActivity(email)
//            } else if (!hasProfilePicture){
//                val intent = Intent(this, IndividualMediaActivity::class.java)
//                startActivity(intent)
//            }else if (!acceptedTerms){
//                moveToTermsAndCondition(accountType)
//            }else{
//                moveToBottomNavigationIndividual()
//            }



            if (profession.isEmpty()) {
                ProfessionDialog(
                    activity = this,
                    authViewModel = authViewModel,
                    onSubmit = { profession ->
                        proceedToNextStep(isVerified,email,hasProfilePicture,acceptedTerms,accountType)
                    },
                    onCancel = {
                        preferenceManager.clearPreferences()
                        googleSignOut()
                    }
                ).show()
            } else {
                proceedToNextStep(isVerified,email,hasProfilePicture,acceptedTerms,accountType)
            }


        }else{
            fullName = result?.data?.businessProfile?.name ?: ""
            userSmallProfilePic = result?.data?.businessProfile?.profilePic?.small ?: ""
            userMediumProfilePic = result?.data?.businessProfile?.profilePic?.medium ?: ""
            userLargeProfilePic = result?.data?.businessProfile?.profilePic?.large ?: ""
            val businessProfile = result?.data?.businessProfile
            val businessTypeID = businessProfile?.businessTypeID.toString()
            val businessSubTypeID = businessProfile?.businessSubTypeID.toString()

            val address = businessProfile?.address
            val street = address?.street.toString()
            val city = address?.city.toString()
            val state = address?.state.toString()
            val country = address?.country.toString()
            val zipCode = address?.zipCode.toString()
            val lat = address?.lat.toString()
            val lng = address?.lng.toString()
            preferenceManager.putString(PreferenceManager.Keys.USER_FULL_NAME, fullName)
            preferenceManager.putString(PreferenceManager.Keys.USER_SMALL_PROFILE_PIC, userSmallProfilePic)
            preferenceManager.putString(PreferenceManager.Keys.USER_MEDIUM_PROFILE_PIC, userMediumProfilePic)
            preferenceManager.putString(PreferenceManager.Keys.USER_LARGE_PROFILE_PIC, userLargeProfilePic)
            preferenceManager.putString(PreferenceManager.Keys.USER_BUSINESS_ID, businessTypeID)
            preferenceManager.putString(PreferenceManager.Keys.USER_SUB_BUSINESS_ID, businessSubTypeID)

            preferenceManager.putString(PreferenceManager.Keys.USER_STREET, street)
            preferenceManager.putString(PreferenceManager.Keys.USER_CITY, city)
            preferenceManager.putString(PreferenceManager.Keys.USER_STATE, state)
            preferenceManager.putString(PreferenceManager.Keys.USER_COUNTRY, country)
            preferenceManager.putString(PreferenceManager.Keys.USER_ZIPCODE, zipCode)
            preferenceManager.putString(PreferenceManager.Keys.USER_LATITUDE, lat)
            preferenceManager.putString(PreferenceManager.Keys.USER_LONGITUDE, lng)

            preferenceManager.putBoolean(PreferenceManager.Keys.BUSINESS_ACC_APPROVED, isApproved)

            // Get business profile creation date for grace period check
            val businessProfileCreatedAt = businessProfile?.createdAt ?: ""
            // Store business profile creation date in preferences for use in other activities
            preferenceManager.putString(PreferenceManager.Keys.BUSINESS_PROFILE_CREATED_AT, businessProfileCreatedAt)

            if (!isVerified){
                moveToBusinessVerifyEmailActivity(email)
            } else if (!hasAmenities){
                moveToBusinessQuestionsActivity()
            }else if (!hasProfilePicture){
                moveToBusinessMediaActivity()
            }else if (!isDocumentUploaded){
                moveToBusinessSupportingDocumentsActivity()
            }else if (!acceptedTerms){
                moveToTermsAndCondition(accountType)
            }else if (!hasSubscription){
                // Check if business profile was created less than 11 months ago (grace period)
                // If within grace period, redirect to home page instead of subscription page
                val isWithinGracePeriod = com.thehotelmedia.android.extensions.isWithinGracePeriod(businessProfileCreatedAt)
                if (isWithinGracePeriod) {
                    // Business user is within 11-month grace period, redirect to home page
                    moveToBottomNavigationBusiness()
                } else {
                    // Grace period has passed, show subscription page
                    moveToBusinessSubscriptionActivity()
                }
            }else{
                moveToBottomNavigationBusiness()
            }

        }


    }

    private fun proceedToNextStep(
        verified: Boolean,
        email: String,
        hasProfilePicture: Boolean,
        acceptedTerms: Boolean,
        accountType: String
    ) {
        if (!verified){
            moveToIndividualVerifyEmailActivity(email)
        } else if (!hasProfilePicture){
            val intent = Intent(this, IndividualMediaActivity::class.java)
            startActivity(intent)
        }else if (!acceptedTerms){
            moveToTermsAndCondition(accountType)
        }else{
            moveToBottomNavigationIndividual()
        }
    }


    private fun moveToIndividualVerifyEmailActivity(email: String) {
        val intent = Intent(this, IndividualVerifyEmailActivity::class.java)
        intent.putExtra("EMAIL_ADDRESS", email)
        startActivity(intent)
    }
    private fun moveToBusinessVerifyEmailActivity(email: String) {
        val intent = Intent(this, BusinessVerifyEmailActivity::class.java)
        intent.putExtra("EMAIL_ADDRESS", email)
        startActivity(intent)
    }

    private fun moveToBottomNavigationIndividual() {
        val intent = Intent(this, BottomNavigationIndividualMainActivity::class.java)
        startActivity(intent)
    }
    private fun moveToBottomNavigationBusiness() {
        val intent = Intent(this, BottomNavigationBusinessMainActivity::class.java)
        startActivity(intent)
    }

    private fun moveToTermsAndCondition(accountType: String) {
        val intent = Intent(this, TermsAndConditionsActivity::class.java)
        intent.putExtra("From", accountType)
        startActivity(intent)
    }
    private fun moveToBusinessQuestionsActivity() {
        val intent = Intent(this, BusinessQuestionsActivity::class.java)
        startActivity(intent)
    }
    private fun moveToBusinessMediaActivity() {
        val intent = Intent(activity, BusinessTypeMediaActivity::class.java)
        startActivity(intent)
    }
    private fun moveToBusinessSupportingDocumentsActivity() {
        val intent = Intent(this, SupportingDocumentsActivity::class.java)
        startActivity(intent)
    }
    private fun moveToBusinessSubscriptionActivity() {
        val intent = Intent(this, BusinessSubscriptionActivity::class.java)
        startActivity(intent)
    }

    private fun login() {
        val email = binding.emailEt.text.toString().trim()
        val password = binding.passwordEt.text.toString().trim()
        authViewModel.login(email, password,deviceId,fcmToken,userLat,userLng,currentLanguage)
    }

    private fun validateFields(
        emailEt: AppCompatEditText,
        passwordEt: AppCompatEditText
    ): ValidationResult {
        val fields = listOf(
            emailEt to ::validateEmail,
            passwordEt to ::validatePassword
        )
        for ((editText, validator) in fields) {
            val result = validator(editText)
            if (!result.isValid) {
//                editText.error = result.errorMessage
                editText.requestFocus()
                return ValidationResult(false, result.errorMessage)
            }
        }

        return ValidationResult(true, null)
    }

    private fun validateEmail(editText: EditText): ValidationResult {
        val text = editText.text.toString().trim()
        return when {
            text.isEmpty() -> ValidationResult(false, "Enter email address.")
            !Patterns.EMAIL_ADDRESS.matcher(text).matches() -> ValidationResult(false, "Enter valid email address.")
            else -> ValidationResult(true, null)
        }
    }

    private fun validatePassword(editText: EditText): ValidationResult {
        val text = editText.text.toString().trim()
        return when {
            text.isEmpty() -> ValidationResult(false, "Enter password.")
            text.length < 8 -> ValidationResult(false, "Password must be at least 8 characters.")
            else -> ValidationResult(true, null)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setPasswordEt() {
        binding.passwordEt.setOnTouchListener { _, event ->
            val DRAWABLE_END = 2
            if (event.action == MotionEvent.ACTION_UP) {
                if (event.rawX >= (binding.passwordEt.right - binding.passwordEt.compoundDrawables[DRAWABLE_END].bounds.width())) {
                    isPasswordVisible = !isPasswordVisible
                    togglePasswordVisibility(isPasswordVisible, binding.passwordEt)
                    return@setOnTouchListener true
                }
            }
            false
        }
    }

    private fun togglePasswordVisibility(isVisible: Boolean, editText: AppCompatEditText) {
        if (isVisible) {
            editText.transformationMethod = HideReturnsTransformationMethod.getInstance()
            editText.setCompoundDrawablesRelativeWithIntrinsicBounds(
                R.drawable.ic_password_icon,
                0,
                R.drawable.ic_show_password,
                0
            )
        } else {
            editText.transformationMethod = PasswordTransformationMethod.getInstance()
            editText.setCompoundDrawablesRelativeWithIntrinsicBounds(
                R.drawable.ic_password_icon,
                0,
                R.drawable.ic_hide_password,
                0
            )
        }
        editText.setSelection(editText.text?.length ?: 0) // Keep the cursor at the end of the text
    }

    private fun setIconAnimation() {
        // Set up the animation
        binding.icon.animate()
            .scaleX(2.2f)  // Scale factor (you can adjust)
            .scaleY(2.2f)   // Scale factor (you can adjust)
            .translationY(200f)  // Translation if needed
            .setDuration(1500)  // Duration of the animation
            .setListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator) {}
                override fun onAnimationEnd(animation: Animator) {
                    binding.icon.invalidate()
                    binding.icon.setImageResource(R.drawable.ic_app_large_logo)
                }
                override fun onAnimationCancel(animation: Animator) {}
                override fun onAnimationRepeat(animation: Animator) {}
            })
            .start()
    }

}