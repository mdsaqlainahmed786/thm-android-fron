package com.thehotelmedia.android.repository

import android.content.Context
import com.thehotelmedia.android.apiService.Retrofit
import com.thehotelmedia.android.customClasses.PreferenceManager
import com.thehotelmedia.android.interFaces.Authentication
import com.thehotelmedia.android.modals.Business.businessType.BusinessTypeModal
import com.thehotelmedia.android.modals.Business.subBusinessType.SubBusinessTypeModal
import com.thehotelmedia.android.modals.authentication.business.BusinessSubscriptionPlans.BusinessSubscriptionPlansModal
import com.thehotelmedia.android.modals.authentication.business.answers.AnswerModal
import com.thehotelmedia.android.modals.authentication.business.businessSignUp.BusinessSignUpModal
import com.thehotelmedia.android.modals.authentication.business.buySubscription.BuySubscriptionModal
import com.thehotelmedia.android.modals.authentication.business.questionAnswer.QuestionAnswerModal
import com.thehotelmedia.android.modals.authentication.business.subscriptionCheckOut.SubscriptionCheckOutModal
import com.thehotelmedia.android.modals.authentication.business.supportingDocuments.DocumentsModal
import com.thehotelmedia.android.modals.authentication.business.uploadPropertyPictureModal.UploadPropertyPictureModal
import com.thehotelmedia.android.modals.authentication.business.verifyEmail.BusinessVerifyEmailModal
import com.thehotelmedia.android.modals.authentication.forgetPassword.forgetPassword.ForgetPasswordModal
import com.thehotelmedia.android.modals.authentication.individual.signUp.IndividualSignUpModal
import com.thehotelmedia.android.modals.authentication.individual.uploadProfilePic.IndividualProfilePicModal
import com.thehotelmedia.android.modals.authentication.individual.verifyEmail.IndividualVerifyEmailModal
import com.thehotelmedia.android.modals.authentication.logOut.LogOutModal
import com.thehotelmedia.android.modals.authentication.login.LoginModal
import com.thehotelmedia.android.modals.authentication.refreshToken.RefreshTokenModal
import com.thehotelmedia.android.requestModel.auth.VerifyOtpRequest
import com.thehotelmedia.android.modals.getProfessions.GetProfessionModal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import java.io.IOException

class AuthRepo (private val context: Context){

    private fun getAccessToken(): String {
        val preferenceManager = PreferenceManager.getInstance(context)
        return preferenceManager.getString(PreferenceManager.Keys.ACCESS_TOKEN,"").toString()
    }

    private fun getCookies(): String {
        val preferenceManager = PreferenceManager.getInstance(context)
        return preferenceManager.getString(PreferenceManager.Keys.COOKIES,"").toString()
    }

    suspend fun individualSignUp(accountType: String, email: String, dialCode: String, phoneNumber: String, fullName: String
                                 , password: String, profession: String, lat: Double, lng: Double,language: String): Response<IndividualSignUpModal> {
        return withContext(Dispatchers.IO) {
            val call = Retrofit.authApiService(context).create(Authentication::class.java)
            return@withContext call.individualSignUp(accountType, email,dialCode,phoneNumber,fullName,password,profession,lat,lng,language).execute()
        }
    }

    suspend fun individualVerifyEmail(email: String, otp: String, deviceID: String, notificationToken: String): Response<IndividualVerifyEmailModal> {
        return withContext(Dispatchers.IO) {
            val call = Retrofit.authApiService(context).create(Authentication::class.java)
            return@withContext call.individualVerifyEmail(email, otp,deviceID,notificationToken,"android").execute()
        }
    }

    suspend fun updateIndividualProfilePic( profilePic: MultipartBody.Part): Response<IndividualProfilePicModal> {
        val accessToken = getAccessToken()
        if (accessToken.isEmpty()) {
            throw IllegalStateException("Access token is null or empty")
        }
        return withContext(Dispatchers.IO) {
            val call = Retrofit.apiService(context).create(Authentication::class.java)
            return@withContext call.uploadIndividualProfilePic(accessToken,profilePic).execute()
        }
    }

    suspend fun termsAndConditions(acceptedTerms: Boolean): Response<IndividualVerifyEmailModal> {
        val accessToken = getAccessToken()
        if (accessToken.isEmpty()) {
            throw IllegalStateException("Access token is null or empty")
        }
        return withContext(Dispatchers.IO) {
            val call = Retrofit.authApiService(context).create(Authentication::class.java)
            return@withContext call.termsAndCondition(accessToken, acceptedTerms).execute()
        }
    }

    suspend fun login(email: String, password: String, deviceID: String, notificationToken: String, lat: Double, lng: Double, language: String): Response<LoginModal> {
        return withContext(Dispatchers.IO) {
            val call = Retrofit.authApiService(context).create(Authentication::class.java)
            return@withContext call.login(email, password,deviceID,notificationToken,"android",lat,lng,language).execute()
        }
    }

    suspend fun socialLogin(socialType: String, token: String, dialCode: String, phoneNumber: String, deviceID: String, notificationToken: String, lat: Double, lng: Double, language: String): Response<LoginModal> {
        return withContext(Dispatchers.IO) {
            val call = Retrofit.authApiService(context).create(Authentication::class.java)
            return@withContext call.socialLogin(socialType, token,dialCode,phoneNumber,deviceID,"android",notificationToken,lat,lng,language).execute()
        }
    }

    suspend fun verifyOtpLogin(dialCode: String, phoneNumber: String, deviceID: String, notificationToken: String, lat: Double, lng: Double, language: String): Response<LoginModal> {
        return withContext(Dispatchers.IO) {
            val call = Retrofit.authApiService(context).create(Authentication::class.java)
            val request = VerifyOtpRequest(
                phoneNumber = phoneNumber,
                dialCode = dialCode,
                deviceID = deviceID,
                devicePlatform = "android",
                notificationToken = notificationToken,
                lat = lat,
                lng = lng,
                language = language
            )
            return@withContext call.verifyOtpLogin(request).execute()
        }
    }

    suspend fun reSendOtp(email: String,type: String,): Response<LoginModal> {
        return withContext(Dispatchers.IO) {
            val call = Retrofit.authApiService(context).create(Authentication::class.java)
            return@withContext call.reSendOtp(email,type).execute()
        }
    }

    suspend fun getBusinessType(): Response<BusinessTypeModal> {
        return withContext(Dispatchers.IO) {
            val call = Retrofit.authApiService(context).create(Authentication::class.java)
            return@withContext call.getBusinessType().execute()
        }
    }

    suspend fun getSubBusinessType(businessId: String): Response<SubBusinessTypeModal> {
        return withContext(Dispatchers.IO) {
            val call = Retrofit.authApiService(context).create(Authentication::class.java)
            return@withContext call.getSubBusinessType(businessId).execute()
        }
    }

    suspend fun getProfession(): Response<GetProfessionModal> {
        return withContext(Dispatchers.IO) {
            val call = Retrofit.authApiService(context).create(Authentication::class.java)
            return@withContext call.getProfession().execute()
        }
    }

    suspend fun businessSignUp(
        email: String, fullName: String, password: String, dialCode: String, phoneNumber: String, businessName: String, businessEmail: String, businessDialCode: String,
        businessPhoneNumber: String, businessType: String, businessSubType: String, businessDescription: String, businessWebsite: String,
        gstn: String, street: String, city: String, state: String, country: String, zipCode: String, lat: String, lng: String,  accountType: String, placeID: String, ): Response<BusinessSignUpModal> {
        return withContext(Dispatchers.IO) {
            val call = Retrofit.authApiService(context).create(Authentication::class.java)
            return@withContext call.businessSignUp(email,fullName, password, dialCode, phoneNumber, businessName, businessEmail, businessDialCode,
                businessPhoneNumber, businessType, businessSubType, businessDescription, businessWebsite, gstn, street, city, state, country, zipCode, lat, lng,accountType,placeID).execute()
        }
    }

    suspend fun businessVerifyEmail(email: String, otp: String, deviceID: String, notificationToken: String): Response<BusinessVerifyEmailModal> {
        return withContext(Dispatchers.IO) {
            val call = Retrofit.authApiService(context).create(Authentication::class.java)
            return@withContext call.businessVerifyEmail(email, otp,deviceID,notificationToken,"android").execute()
        }
    }

    suspend fun getQuestionAnswer(businessTypeID: String, businessSubtypeID: String): Response<QuestionAnswerModal> {
        val accessToken = getAccessToken()
        if (accessToken.isEmpty()) {
            throw IllegalStateException("Access token is null or empty")
        }
        return withContext(Dispatchers.IO) {
            val call = Retrofit.authApiService(context).create(Authentication::class.java)
            return@withContext call.getQuestionAnswer(accessToken,businessTypeID, businessSubtypeID).execute()
        }
    }

    suspend fun answerQuestion(requestBody: RequestBody): Response<AnswerModal> {
        val accessToken = getAccessToken()
        if (accessToken.isEmpty()) {
            throw IllegalStateException("Access token is null or empty")
        }
        return withContext(Dispatchers.IO) {
            val call = Retrofit.apiService(context).create(Authentication::class.java)
            return@withContext call.answerQuestion(accessToken,requestBody).execute()
        }
    }

    suspend fun supportingDocuments( businessRegistration: MultipartBody.Part,addressProof: MultipartBody.Part): Response<DocumentsModal> {
        val accessToken = getAccessToken()
        if (accessToken.isEmpty()) {
            throw IllegalStateException("Access token is null or empty")
        }
        return withContext(Dispatchers.IO) {
            val call = Retrofit.apiService(context).create(Authentication::class.java)
            return@withContext call.supportingDocuments(accessToken,businessRegistration,addressProof).execute()
        }
    }

    suspend fun getSubscriptionPlans(): Response<BusinessSubscriptionPlansModal> {
        val accessToken = getAccessToken()
        if (accessToken.isEmpty()) {
            throw IllegalStateException("Access token is null or empty")
        }
        return withContext(Dispatchers.IO) {
            val call = Retrofit.apiService(context).create(Authentication::class.java)
            return@withContext call.getSubscriptionPlans(accessToken).execute()
        }
    }

    suspend fun subscriptionCheckOut(subscriptionPlanID: String,promoCode: String): Response<SubscriptionCheckOutModal> {
        val accessToken = getAccessToken()
        if (accessToken.isEmpty()) {
            throw IllegalStateException("Access token is null or empty")
        }
        return withContext(Dispatchers.IO) {
            val call = Retrofit.authApiService(context).create(Authentication::class.java)
            return@withContext call.subscriptionCheckOut(accessToken,subscriptionPlanID,promoCode).execute()
        }
    }

    suspend fun buySubscription(orderID: String,paymentID: String,signature: String): Response<BuySubscriptionModal> {
        val accessToken = getAccessToken()
        if (accessToken.isEmpty()) {
            throw IllegalStateException("Access token is null or empty")
        }
        return withContext(Dispatchers.IO) {
            val call = Retrofit.authApiService(context).create(Authentication::class.java)
            return@withContext call.buySubscription(accessToken,orderID,paymentID,signature).execute()
        }
    }

    suspend fun subscriptionVerifyByGoogle(purchaseToken: String,subscriptionId: String,orderId: String): Response<BuySubscriptionModal> {
        val accessToken = getAccessToken()
        if (accessToken.isEmpty()) {
            throw IllegalStateException("Access token is null or empty")
        }
        return withContext(Dispatchers.IO) {
            val call = Retrofit.authApiService(context).create(Authentication::class.java)
            return@withContext call.subscriptionVerifyByGoogle(accessToken,purchaseToken,subscriptionId,orderId).execute()
        }
    }

    suspend fun logOut(): Response<LogOutModal> {
        val cookies = getCookies()
        if (cookies.isEmpty()) {
            throw IllegalStateException("Cookies is null or empty")
        }
        return withContext(Dispatchers.IO) {
            val call = Retrofit.apiService(context).create(Authentication::class.java)
            return@withContext call.logOut(cookies,"Demo").execute()
        }
    }

    suspend fun refreshToken(): Response<RefreshTokenModal> {
        val cookies = getCookies()
        if (cookies.isEmpty()) {
            throw IllegalStateException("Cookies is null or empty")
        }
        return withContext(Dispatchers.IO) {
            val call = Retrofit.authApiService(context).create(Authentication::class.java)
            return@withContext call.refreshToken(cookies,"Demo").execute()
        }
    }

    suspend fun forgotPassword(email: String): Response<ForgetPasswordModal> {
        return withContext(Dispatchers.IO) {
            val call = Retrofit.authApiService(context).create(Authentication::class.java)
            return@withContext call.forgotPassword(email).execute()
        }
    }

    suspend fun verifyForgotPassword(email: String,otp: String): Response<ForgetPasswordModal> {
        return withContext(Dispatchers.IO) {
            val call = Retrofit.authApiService(context).create(Authentication::class.java)
            return@withContext call.verifyForgotPassword(email,otp).execute()
        }
    }

    suspend fun changePassword(password: String,resetToken: String,email: String): Response<ForgetPasswordModal> {
        return withContext(Dispatchers.IO) {
            val call = Retrofit.authApiService(context).create(Authentication::class.java)
            return@withContext call.changePassword(password,resetToken,email).execute()
        }
    }


    suspend fun editProfession(profession: String): Response<IndividualVerifyEmailModal> {
        val accessToken = getAccessToken()
        if (accessToken.isEmpty()) {
            throw IllegalStateException("Access token is null or empty")
        }
        return withContext(Dispatchers.IO) {
            val call = Retrofit.authApiService(context).create(Authentication::class.java)
            return@withContext call.editProfession(accessToken, profession).execute()
        }
    }

    suspend fun uploadPropertyPicture(imageList: List<MultipartBody.Part>): Response<UploadPropertyPictureModal> {
        val accessToken = getAccessToken()
        if (accessToken.isEmpty()) {
            throw IllegalStateException("Access token is null or empty")
        }
        // Convert strings to RequestBody for text fields
        val accessTokenBody = accessToken.toRequestBody("text/plain".toMediaTypeOrNull())
        // Make the API call
        return withContext(Dispatchers.IO) {
            try {
                val authenticationService = Retrofit.apiService(context).create(Authentication::class.java)
                authenticationService.uploadPropertyPicture(accessTokenBody, imageList).execute()
            } catch (e: Exception) {
                throw IOException("Failed to upload images: ${e.message}", e)
            }
        }
    }

    suspend fun deleteAccount(): Response<LogOutModal> {
        val accessToken = getAccessToken()
        if (accessToken.isEmpty()) {
            throw IllegalStateException("AccessToken is null or empty")
        }
        return withContext(Dispatchers.IO) {
            val call = Retrofit.apiService(context).create(Authentication::class.java)
            return@withContext call.deleteAccount(accessToken).execute()
        }
    }

    suspend fun deactivateAccount(): Response<LogOutModal> {
        val accessToken = getAccessToken()
        if (accessToken.isEmpty()) {
            throw IllegalStateException("AccessToken is null or empty")
        }
        return withContext(Dispatchers.IO) {
            val call = Retrofit.apiService(context).create(Authentication::class.java)
            return@withContext call.deactivateAccount(accessToken).execute()
        }
    }

}