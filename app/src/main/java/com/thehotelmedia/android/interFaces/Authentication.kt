package com.thehotelmedia.android.interFaces

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
import com.thehotelmedia.android.modals.getProfessions.GetProfessionModal
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

interface Authentication {

    @POST("auth/signup")
    @FormUrlEncoded
    fun individualSignUp(
        @Field("accountType") accountType: String,
        @Field("email") email: String,
        @Field("dialCode") dialCode: String,
        @Field("phoneNumber") phoneNumber: String,
        @Field("name") fullName: String,
        @Field("password") password: String,
        @Field("profession") profession: String,
        @Field("lat") lat: Double,
        @Field("lng") lng: Double,
        @Field("language") language: String,
    ): Call<IndividualSignUpModal>

    @POST("auth/email-verify")
    @FormUrlEncoded
    fun individualVerifyEmail(
        @Field("email") email: String,
        @Field("otp") otp: String,
        @Field("deviceID") deviceID: String,
        @Field("notificationToken") notificationToken: String,
        @Field("devicePlatform") devicePlatform: String
    ): Call<IndividualVerifyEmailModal>

    @POST("user/edit-profile-pic")
    @Multipart
    fun uploadIndividualProfilePic(
        @Header("x-access-token") token: String,
        @Part profilePic: MultipartBody.Part?,
    ): Call<IndividualProfilePicModal>

    @PATCH("user/edit-profile")
    @FormUrlEncoded
    fun termsAndCondition(
        @Header("x-access-token") token: String,
        @Field("acceptedTerms") acceptedTerms: Boolean,
    ): Call<IndividualVerifyEmailModal>

    @POST("auth/login")
    @FormUrlEncoded
    fun login(
        @Field("email") email: String,
        @Field("password") password: String,
        @Field("deviceID") deviceID: String,
        @Field("notificationToken") notificationToken: String,
        @Field("devicePlatform") devicePlatform: String,
        @Field("lat") lat: Double,
        @Field("lng") lng: Double,
        @Field("language") language: String
    ): Call<LoginModal>

    @POST("auth/social/login")
    @FormUrlEncoded
    fun socialLogin(
        @Field("socialType") socialType: String,
        @Field("token") token: String,
        @Field("dialCode") dialCode: String,
        @Field("phoneNumber") phoneNumber: String,
        @Field("deviceID") deviceID: String,
        @Field("devicePlatform") devicePlatform: String,
        @Field("notificationToken") notificationToken: String,
        @Field("lat") lat: Double,
        @Field("lng") lng: Double,
        @Field("language") language: String
    ): Call<LoginModal>

    @POST("auth/resend-otp")
    @FormUrlEncoded
    fun reSendOtp(
        @Field("email") email: String,
        @Field("type") type: String,
    ): Call<LoginModal>

    //// For Business
    @GET("business/types")
    fun getBusinessType(
    ): Call<BusinessTypeModal>

    @GET("business/subtypes"+"/{id}")
    fun getSubBusinessType(
        @Path(value = "id",encoded = true) businessId: String,
    ): Call<SubBusinessTypeModal>

    @GET("professions")
    fun getProfession(): Call<GetProfessionModal>

    @POST("auth/email-verify")
    @FormUrlEncoded
    fun businessVerifyEmail(
        @Field("email") email: String,
        @Field("otp") otp: String,
        @Field("deviceID") deviceID: String,
        @Field("notificationToken") notificationToken: String,
        @Field("devicePlatform") devicePlatform: String
    ): Call<BusinessVerifyEmailModal>

    @POST("auth/signup")
    @FormUrlEncoded
    fun businessSignUp(
        @Field("email") email: String,
        @Field("name") fullName: String,
        @Field("password") password: String,
        @Field("dialCode") dialCode: String,
        @Field("phoneNumber") phoneNumber: String,
        @Field("businessName") businessName: String,
        @Field("businessEmail") businessEmail: String,
        @Field("businessDialCode") businessDialCode: String,
        @Field("businessPhoneNumber") businessPhoneNumber: String,
        @Field("businessType") businessType: String,
        @Field("businessSubType") businessSubType: String,
        @Field("bio") businessDescription: String,
        @Field("businessWebsite") businessWebsite: String,
        @Field("gstn") gstn: String,
        @Field("street") street: String,
        @Field("city") city: String,
        @Field("state") state: String,
        @Field("country") country: String,
        @Field("zipCode") zipCode: String,
        @Field("lat") lat: String,
        @Field("lng") lng: String,
        @Field("accountType") accountType: String,
        @Field("placeID") placeID: String,
    ): Call<BusinessSignUpModal>

    @POST("business/questions")
    @FormUrlEncoded
    fun getQuestionAnswer(
        @Header("x-access-token") token: String,
        @Field("businessTypeID") businessTypeID: String,
        @Field("businessSubtypeID") businessSubtypeID: String,
    ): Call<QuestionAnswerModal>

    @POST("business/questions/answers")
    fun answerQuestion(
        @Header("x-access-token") token: String,
        @Body requestBody: RequestBody,
    ): Call<AnswerModal>

    @POST("user/business-profile/documents")
    @Multipart
    fun supportingDocuments(
        @Header("x-access-token") token: String,
        @Part businessRegistration: MultipartBody.Part?,
        @Part addressProof: MultipartBody.Part?,
    ): Call<DocumentsModal>

    @GET("user/subscription/plans")
    fun getSubscriptionPlans(
        @Header("x-access-token") token: String,
    ): Call<BusinessSubscriptionPlansModal>

    @POST("user/subscription/checkout")
    @FormUrlEncoded
    fun subscriptionCheckOut(
        @Header("x-access-token") token: String,
        @Field("subscriptionPlanID") subscriptionPlanID: String,
        @Field("promoCode") promoCode: String,
    ): Call<SubscriptionCheckOutModal>

    @POST("user/subscription")
    @FormUrlEncoded
    fun buySubscription(
        @Header("x-access-token") token: String,
        @Field("orderID") orderID: String,
        @Field("paymentID") paymentID: String,
        @Field("signature") signature: String
    ): Call<BuySubscriptionModal>

    @POST("google/purchases/subscriptions/verify")
    @FormUrlEncoded
    fun subscriptionVerifyByGoogle(
        @Header("x-access-token") token: String,
        @Field("token") purchaseToken: String,
        @Field("subscriptionID") subscriptionID: String,
        @Field("orderID") orderID: String,
    ): Call<BuySubscriptionModal>

    @POST("auth/logout")
    @FormUrlEncoded
    fun logOut(
        @Header("Cookie") cookie: String,  // Add the cookie header
        @Field("demo") demo: String,
    ): Call<LogOutModal>

    @POST("auth/refresh-token")
    @FormUrlEncoded
    fun refreshToken(
        @Header("Cookie") cookie: String,  // Add the cookie header
        @Field("demo") demo: String,
    ): Call<RefreshTokenModal>

    //ForgetPassword
    @POST("auth/forgot-password")
    @FormUrlEncoded
    fun forgotPassword(
        @Field("email") email: String,
    ): Call<ForgetPasswordModal>

    @POST("auth/forgot-password/verify-otp")
    @FormUrlEncoded
    fun verifyForgotPassword(
        @Field("email") email: String,
        @Field("otp") otp: String,
    ): Call<ForgetPasswordModal>

    @POST("auth/reset-password")
    @FormUrlEncoded
    fun changePassword(
        @Field("password") password: String,
        @Field("resetToken") resetToken: String,
        @Field("email") email: String,
    ): Call<ForgetPasswordModal>

    @PATCH("user/edit-profile")
    @FormUrlEncoded
    fun editProfession(
        @Header("x-access-token") token: String,
        @Field("profession") profession: String,
    ): Call<IndividualVerifyEmailModal>

    @Multipart
    @POST("user/business-profile/property-picture")
    fun uploadPropertyPicture(
        @Header("x-access-token") token: RequestBody,
        @Part images: List<MultipartBody.Part>, // List for images
    ): Call<UploadPropertyPictureModal>

    @DELETE("user/account")
    fun deleteAccount(
        @Header("x-access-token") token: String,
    ): Call<LogOutModal>

    @PATCH("user/account/disable")
    fun deactivateAccount(
        @Header("x-access-token") token: String,
    ): Call<LogOutModal>

}