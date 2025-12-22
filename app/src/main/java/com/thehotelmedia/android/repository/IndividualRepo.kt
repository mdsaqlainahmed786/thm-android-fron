package com.thehotelmedia.android.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import com.google.gson.Gson
import com.thehotelmedia.android.BuildConfig
import com.thehotelmedia.android.SocketModals.sendMedia.SendMediaModal
import com.thehotelmedia.android.apiService.Retrofit
import com.thehotelmedia.android.customClasses.PreferenceManager
import com.thehotelmedia.android.interFaces.Application
import com.thehotelmedia.android.modals.DeleteModal
import com.thehotelmedia.android.modals.SharePostModal
import com.thehotelmedia.android.modals.Stories.StoriesModal
import com.thehotelmedia.android.modals.accountReach.AccountReachModal
import com.thehotelmedia.android.modals.authentication.verifyMobileNumber.VerifyMobileNumberModal
import com.thehotelmedia.android.modals.blockUser.BlockUserModel
import com.thehotelmedia.android.modals.booking.acceptReject.AcceptRejectBookingModal
import com.thehotelmedia.android.modals.booking.bookBanquet.BookBanquetModal
import com.thehotelmedia.android.modals.booking.bookRoom.BookRoomModal
import com.thehotelmedia.android.modals.booking.bookTable.BookTableModal
import com.thehotelmedia.android.modals.booking.bookingHistory.BookingHistoryModal
import com.thehotelmedia.android.modals.booking.bookingSummary.BookingSummaryModal
import com.thehotelmedia.android.modals.booking.checkIn.BookingCheckInModal
import com.thehotelmedia.android.modals.booking.checkout.BookingCheckOutModal
import com.thehotelmedia.android.modals.booking.roomDetails.RoomDetailsModal
import com.thehotelmedia.android.modals.chat.exportChat.ExportChatModal
import com.thehotelmedia.android.modals.checkIn.NearByPlacesModel
import com.thehotelmedia.android.modals.checkinData.checkInData.CheckInDataModel
import com.thehotelmedia.android.modals.editProfile.EditProfileModal
import com.thehotelmedia.android.modals.helpAndSupport.faqs.FAQsModal
import com.thehotelmedia.android.modals.feeds.createComment.CreateCommentModal
import com.thehotelmedia.android.modals.feeds.feed.FeedModal
import com.thehotelmedia.android.modals.feeds.getComments.GetCommentModal
import com.thehotelmedia.android.modals.feeds.savePost.SavePostModal
import com.thehotelmedia.android.modals.followAction.FollowActionModal
import com.thehotelmedia.android.modals.followUser.FollowUserModal
import com.thehotelmedia.android.modals.followUser.UnfollowModel
import com.thehotelmedia.android.modals.followerFollowing.FollowFollowingModal
import com.thehotelmedia.android.modals.forms.createEvent.CreateEventModal
import com.thehotelmedia.android.modals.forms.createPost.CreatePostModal
import com.thehotelmedia.android.modals.forms.createReviews.CreateReviewModel
import com.thehotelmedia.android.modals.forms.createStory.CreateStoryModal
import com.thehotelmedia.android.modals.forms.taggedPeople.TaggedPeopleModal
import com.thehotelmedia.android.modals.getBusinessDoc.GetBusinessDocumentsModal
import com.thehotelmedia.android.modals.helpAndSupport.contactUs.ContactUsModal
import com.thehotelmedia.android.modals.insight.InsightModal
import com.thehotelmedia.android.modals.job.jobDetails.JobDetailsModal
import com.thehotelmedia.android.modals.notificationStatus.NotificationStatusModal
import com.thehotelmedia.android.modals.notifications.NotificationModal
import com.thehotelmedia.android.modals.job.postJob.PostJobModal
import com.thehotelmedia.android.modals.profileData.image.ImageModal
import com.thehotelmedia.android.modals.profileData.profile.GetProfileModal
import com.thehotelmedia.android.modals.profileData.video.VideoModal
import com.thehotelmedia.android.modals.report.ReportUserModal
import com.thehotelmedia.android.modals.search.SearchModel
import com.thehotelmedia.android.modals.share.shareProfile.ShareProfileModal
import com.thehotelmedia.android.modals.storiesActions.DeleteStoryModal
import com.thehotelmedia.android.modals.storiesActions.likeStory.LikesModal
import com.thehotelmedia.android.modals.storiesActions.publishStory.PublishStoryModal
import com.thehotelmedia.android.modals.storiesActions.likeViewStory.StoryActionModal
import com.thehotelmedia.android.modals.subscriptionDetails.SubscriptionData
import com.thehotelmedia.android.modals.subscriptions.CancelSubscriptions
import com.thehotelmedia.android.modals.subscriptions.SubscriptionsModal
import com.thehotelmedia.android.modals.suggestedBusiness.SuggestedBusinessModal
import com.thehotelmedia.android.modals.transactions.TransactionModal
import com.thehotelmedia.android.modals.updateAddress.UpdateAddressModal
import com.thehotelmedia.android.modals.userProfile.UserProfileModel
import com.thehotelmedia.android.modals.viewMedia.ViewMediaModal
import com.thehotelmedia.android.modals.viewPostEvent.ViewPostEventModal
import com.thehotelmedia.android.modals.visitWebSite.WebsiteRedirectModal
import com.thehotelmedia.android.modals.collaboration.CollaborationActionModal
import com.thehotelmedia.android.modals.collaboration.CollaborationPostsModal
import com.thehotelmedia.android.modals.collaboration.CollaboratorsListModal
import com.thehotelmedia.android.modals.menu.MenuResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import java.io.File
import java.nio.file.Files


class IndividualRepo (private val context: Context){

    private fun getAccessToken(): String {
        val preferenceManager = PreferenceManager.getInstance(context)
        return preferenceManager.getString(PreferenceManager.Keys.ACCESS_TOKEN,"").toString()
    }
    private fun getCookies(): String {
        val preferenceManager = PreferenceManager.getInstance(context)
        return preferenceManager.getString(PreferenceManager.Keys.COOKIES,"").toString()
    }

    private val googleApiKey = BuildConfig.MAPS_API_KEY

    suspend fun getSubscriptionDetails(): Response<SubscriptionData> {
        val accessToken = getAccessToken()
        if (accessToken.isEmpty()) {
            throw IllegalStateException("Access token is null or empty")
        }
        return withContext(Dispatchers.IO) {
            val call = Retrofit.apiService(context).create(Application::class.java)
            return@withContext call.getSubscriptionDetails(accessToken).execute()
        }
    }

    suspend fun deleteChat(userID: String): Response<DeleteModal> {
        val accessToken = getAccessToken()
        if (accessToken.isEmpty()) {
            throw IllegalStateException("Access token is null or empty")
        }
        return withContext(Dispatchers.IO) {
            val call = Retrofit.apiService(context).create(Application::class.java)
            return@withContext call.deleteChat(accessToken,userID).execute()
        }
    }

    suspend fun deletePost(postID: String): Response<DeleteModal> {
        val accessToken = getAccessToken()
        if (accessToken.isEmpty()) {
            throw IllegalStateException("Access token is null or empty")
        }
        return withContext(Dispatchers.IO) {
            val call = Retrofit.apiService(context).create(Application::class.java)
            return@withContext call.deletePost(accessToken,postID).execute()
        }
    }

    suspend fun exportChat(userID: String): Response<ExportChatModal> {
        val accessToken = getAccessToken()
        if (accessToken.isEmpty()) {
            throw IllegalStateException("Access token is null or empty")
        }
        return withContext(Dispatchers.IO) {
            val call = Retrofit.apiService(context).create(Application::class.java)
            return@withContext call.exportChat(accessToken,userID).execute()
        }
    }

    suspend fun viewMedia(postId: String,mediaId: String): Response<ViewMediaModal> {
        val accessToken = getAccessToken()
        if (accessToken.isEmpty()) {
            throw IllegalStateException("Access token is null or empty")
        }
        return withContext(Dispatchers.IO) {
            val call = Retrofit.apiService(context).create(Application::class.java)
            return@withContext call.viewMedia(accessToken,postId,mediaId).execute()
        }
    }

    suspend fun postViews(postIds: List<String>): Response<DeleteModal> {
        println("fjasdkljfklas   $postIds")
        val accessToken = getAccessToken()
        if (accessToken.isEmpty()) {
            throw IllegalStateException("Access token is null or empty")
        }
        return withContext(Dispatchers.IO) {
            val call = Retrofit.apiService(context).create(Application::class.java)
            return@withContext call.postViews(accessToken,postIds).execute()
        }
    }

    suspend fun updatePost(postID: String, content: String, feelings: String, media: List<String>, deletedMedia: List<String>, placeName: String = "", lat: Double = 0.0, lng: Double = 0.0): Response<DeleteModal> {
        val accessToken = getAccessToken()
        if (accessToken.isEmpty()) {
            throw IllegalStateException("Access token is null or empty")
        }
        return withContext(Dispatchers.IO) {
            val call = Retrofit.apiService(context).create(Application::class.java)
            
            // Convert strings to RequestBody
            val contentBody = content.toRequestBody("text/plain".toMediaTypeOrNull())
            val feelingsBody = feelings.toRequestBody("text/plain".toMediaTypeOrNull())
            
            // Create location RequestBody if provided
            val placeNameBody = if (placeName.isNotEmpty()) {
                placeName.toRequestBody("text/plain".toMediaTypeOrNull())
            } else {
                null
            }
            val latBody = if (lat != 0.0) {
                lat.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            } else {
                null
            }
            val lngBody = if (lng != 0.0) {
                lng.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            } else {
                null
            }
            
            // Create deletedMedia JSON array as RequestBody
            val deletedMediaJson = if (deletedMedia.isNotEmpty()) {
                val jsonArray = deletedMedia.joinToString(",", "[", "]") { "\"$it\"" }
                jsonArray.toRequestBody("application/json".toMediaTypeOrNull())
            } else {
                null
            }
            
            // Convert media URIs to MultipartBody.Part
            val mediaParts = media.mapIndexed { index, mediaPath ->
                val file = when {
                    mediaPath.startsWith("content://") -> {
                        val filename = "temp_image_$index.jpg"
                        getFileFromContentUri(context, Uri.parse(mediaPath), filename)
                    }
                    mediaPath.startsWith("file://") -> {
                        File(mediaPath.replace("file://", ""))
                    }
                    else -> {
                        File(mediaPath) // Direct path
                    }
                }
                requireNotNull(file) { "File conversion failed for path: $mediaPath" }
                val mimeType = Files.probeContentType(file.toPath()) ?: "image/*"
                val requestBody = file.asRequestBody(mimeType.toMediaTypeOrNull())
                MultipartBody.Part.createFormData("media", file.name, requestBody)
            }
            
            return@withContext call.updatePost(accessToken, postID, contentBody, feelingsBody, deletedMediaJson, mediaParts, placeNameBody, latBody, lngBody).execute()
        }
    }

    suspend fun shareProfileData(id: String,userID: String): Response<ShareProfileModal> {
        val accessToken = getAccessToken()
        if (accessToken.isEmpty()) {
            throw IllegalStateException("Access token is null or empty")
        }
        return withContext(Dispatchers.IO) {
            val call = Retrofit.apiService(context).create(Application::class.java)
            return@withContext call.shareProfileData(accessToken,id,userID).execute()
        }
    }

    suspend fun getProfile(): Response<GetProfileModal> {
        val accessToken = getAccessToken()
        if (accessToken.isEmpty()) {
            throw IllegalStateException("Access token is null or empty")
        }
        return withContext(Dispatchers.IO) {
            val call = Retrofit.apiService(context).create(Application::class.java)
            return@withContext call.getProfile(accessToken).execute()
        }
    }

    suspend fun updateLanguage(language: String): Response<EditProfileModal> {
        val accessToken = getAccessToken()
        if (accessToken.isEmpty()) {
            throw IllegalStateException("Access token is null or empty")
        }
        return withContext(Dispatchers.IO) {
            val call = Retrofit.apiService(context).create(Application::class.java)
            return@withContext call.updateLanguage(accessToken,language).execute()
        }
    }

    suspend fun editProfile(name: String,email: String,dialCode: String,phoneNumber: String,bio: String): Response<EditProfileModal> {
        val accessToken = getAccessToken()
        if (accessToken.isEmpty()) {
            throw IllegalStateException("Access token is null or empty")
        }
        return withContext(Dispatchers.IO) {
            val call = Retrofit.apiService(context).create(Application::class.java)
            return@withContext call.editProfile(accessToken,name,email,dialCode, phoneNumber, bio).execute()
        }
    }

    suspend fun updateStatus(privateAccount: Boolean,notificationEnabled: Boolean): Response<EditProfileModal> {
        val accessToken = getAccessToken()
        if (accessToken.isEmpty()) {
            throw IllegalStateException("Access token is null or empty")
        }
        return withContext(Dispatchers.IO) {
            val call = Retrofit.apiService(context).create(Application::class.java)
            return@withContext call.updateStatus(accessToken,privateAccount,notificationEnabled).execute()
        }
    }

    suspend fun editCategory(businessTypeID: String,businessSubTypeID: String): Response<EditProfileModal> {
        val accessToken = getAccessToken()
        if (accessToken.isEmpty()) {
            throw IllegalStateException("Access token is null or empty")
        }
        return withContext(Dispatchers.IO) {
            val call = Retrofit.apiService(context).create(Application::class.java)
            return@withContext call.editCategory(accessToken,businessTypeID,businessSubTypeID).execute()
        }
    }

//    suspend fun getNearbyPlaces(location: String): Response<NearByPlacesModel> {
//        return withContext(Dispatchers.IO) {
//            val call = Retrofit.googleApiService(context).create(Application::class.java)
//            return@withContext call.getNearbyPlaces(location,5000,"restaurant",googleApiKey).execute()
//        }
//    }
//suspend fun getNearbyPlaces(location: String): Call<NearByPlacesModel> {
//    return Retrofit.googleApiService.getNearbyPlaces(location, 5000, "restaurant", googleApiKey)
//}

    suspend fun getNearbyPlaces(lat: String,lng: String): Response<NearByPlacesModel> {

        val location = "$lat,$lng"
        return Retrofit.googleApiService.getNearbyPlaces(location, 5000, "restaurant|bar|hotel|night_club", googleApiKey)
    }

    suspend fun getCheckInData(placeId: String,businessProfileID: String): Response<CheckInDataModel> {
        val accessToken = getAccessToken()
        if (accessToken.isEmpty()) {
            throw IllegalStateException("Access token is null or empty")
        }
        return withContext(Dispatchers.IO) {
            val call = Retrofit.apiService(context).create(Application::class.java)
            return@withContext call.getCheckInData(accessToken,placeId,businessProfileID).execute()
        }
    }

    suspend fun createNewPost(
        content: String, placeName: String, lat: Double, lng: Double, feelings: String, mediaList: List<String>, taggedList: List<String>
    ): Response<CreatePostModal> {
        println("asdfhjkasfhjkas   $mediaList")
        val accessToken = getAccessToken()
        if (accessToken.isEmpty()) {
            throw IllegalStateException("Access token is null or empty")
        }
        // Convert strings to RequestBody for text fields
        val accessTokenBody = accessToken.toRequestBody("text/plain".toMediaTypeOrNull())
        val contentBody = content.toRequestBody("text/plain".toMediaTypeOrNull())
        val placeNameBody = placeName.toRequestBody("text/plain".toMediaTypeOrNull())
        val latBody = lat.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        val lngBody = lng.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        val feelingsBody = feelings.toRequestBody("text/plain".toMediaTypeOrNull())
//        // Function to prepare the media list
//        fun prepareMediaParts(mediaList: List<String>): List<MultipartBody.Part> {
//            return mediaList.map { mediaPath ->
//                val validPath = mediaPath.replace("file:", "")
//                val file = File(validPath)
//
//                // Determine the MIME type (image or video)
//                val mimeType = Files.probeContentType(file.toPath()) ?: "application/octet-stream"
//
//                // Create the request body for the file
//                val requestBody = file.asRequestBody(mimeType.toMediaTypeOrNull())
//
//                // Create MultipartBody.Part for the file with the same part name "media"
//                MultipartBody.Part.createFormData("media", file.name, requestBody)
//            }
//        }
//        val mediaParts = prepareMediaParts(mediaList) // Convert mediaList to MultipartBody.Part list
        val mediaParts = mediaList.map { mediaPath ->
            val validPath = mediaPath.replace("file:", "")
            val file = File(validPath)
            val mimeType = Files.probeContentType(file.toPath()) ?: "application/octet-stream"
            val requestBody = file.asRequestBody(mimeType.toMediaTypeOrNull())

            MultipartBody.Part.createFormData("media", file.name, requestBody)
        }
        // Convert tagged list to MultipartBody.Part (assuming the tagged data is a list of strings)
        val taggedParts = taggedList.map { tagId ->
            MultipartBody.Part.createFormData("tagged[]", tagId)
        }
        // Make the API call
        return withContext(Dispatchers.IO) {
            val call = Retrofit.apiService(context).create(Application::class.java)
            call.createPost(accessTokenBody,contentBody, placeNameBody, latBody, lngBody, feelingsBody, taggedParts, mediaParts).execute()
        }
    }

    suspend fun createReview(
        businessProfileId: String,
        content: String,
        placeId: String,
        reviews: List<Map<String, Any>>,
        name: String,
        street: String,
        city: String,
        state: String,
        zipCode: String,
        country: String,
        lat: Double,
        lng: Double,
        imageList: MutableList<String>,
        videoList: MutableList<String>,
        anonymousUserID: String
    ): Response<CreateReviewModel> {
        val accessToken = getAccessToken()
        if (accessToken.isEmpty()) {
            throw IllegalStateException("Access token is null or empty")
        }
        println("asfkjsakhdfklasdjk   businessProfileId -> $businessProfileId  ,   anonymousUserID -> $anonymousUserID")
        val gson = Gson()
        // Convert each review (Map) to a JSON string
        val reviewsBody = reviews.map { review ->
            // Convert the Map to a JSON string
            val reviewJson = gson.toJson(review)  // This will give you a JSON string like {"questionID": "6762bb46df5a52e965ed8be9", "rating": 5}
            MultipartBody.Part.createFormData("reviews[]", reviewJson)
        }
        val accessTokenBody = accessToken.toRequestBody("text/plain".toMediaTypeOrNull())
        val businessProfileIdBody = businessProfileId.toRequestBody("text/plain".toMediaTypeOrNull())
        val contentBody = content.toRequestBody("text/plain".toMediaTypeOrNull())
        val placeIdBody = placeId.toRequestBody("text/plain".toMediaTypeOrNull())
        val nameBody = name.toRequestBody("text/plain".toMediaTypeOrNull())
        val streetBody = street.toRequestBody("text/plain".toMediaTypeOrNull())
        val cityBody = city.toRequestBody("text/plain".toMediaTypeOrNull())
        val stateBody = state.toRequestBody("text/plain".toMediaTypeOrNull())
        val zipCodeBody = zipCode.toRequestBody("text/plain".toMediaTypeOrNull())
        val countryBody = country.toRequestBody("text/plain".toMediaTypeOrNull())
        val latBody = lat.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        val lngBody = lng.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        // Convert image paths to MultipartBody.Part
//        val imageParts = imageList.map { imagePath ->
//            val validPath = imagePath.replace("file:", "")
//            val file = File(validPath)
//            val mimeType = Files.probeContentType(file.toPath()) ?: "image/*" // Dynamic MIME type
//            val requestBody = file.asRequestBody(mimeType.toMediaTypeOrNull())
//            MultipartBody.Part.createFormData("images", file.name, requestBody)
//        }

        val imageParts = imageList.mapIndexed { index, mediaPath ->
            val file = when {
                mediaPath.startsWith("content://") -> {
                    val filename = "temp_image_$index.jpg"
                    getFileFromContentUri(context, Uri.parse(mediaPath), filename)
                }
                mediaPath.startsWith("file://") -> {
                    File(mediaPath.replace("file://", ""))
                }
                else -> {
                    File(mediaPath) // Direct path
                }
            }
            requireNotNull(file) { "File conversion failed for path: $mediaPath" }
            val mimeType = Files.probeContentType(file.toPath()) ?: "image/*"
            val requestBody = file.asRequestBody(mimeType.toMediaType())
            MultipartBody.Part.createFormData("images", file.name, requestBody)
        }



//        val imageParts = imageList.map { mediaPath ->
//            val file = when {
//                mediaPath.startsWith("content://") -> {
//                    val filename = "temp_image.jpg"
//                    getFileFromContentUri(context, Uri.parse(mediaPath), filename)
//                }
//                mediaPath.startsWith("file://") -> {
//                    File(mediaPath.replace("file://", ""))
//                }
//                else -> {
//                    File(mediaPath) // Direct path
//                }
//            }
//            // Ensure file is not null
//            requireNotNull(file) { "File conversion failed for path: $mediaPath" }
//            // Ensure MIME type is never null
//            val mimeType = Files.probeContentType(file.toPath()) ?: "image/*"
//            val requestBody = file.asRequestBody(mimeType.toMediaType()) // Ensure non-null MIME type
//            MultipartBody.Part.createFormData("images", file.name, requestBody)
//        }

        // Convert video paths to MultipartBody.Part
        val videoParts = videoList.map { videoPath ->
            val validPath = videoPath.replace("file:", "")
            val file = File(validPath)
            val mimeType = Files.probeContentType(file.toPath()) ?: "video/*" // Dynamic MIME type
            val requestBody = file.asRequestBody(mimeType.toMediaTypeOrNull())
            MultipartBody.Part.createFormData("videos", file.name, requestBody)
        }

        val anonymousUserIDBody = anonymousUserID.toRequestBody("text/plain".toMediaTypeOrNull())
        return withContext(Dispatchers.IO) {
            val call = Retrofit.apiService(context).create(Application::class.java)
            return@withContext call.createReview(accessTokenBody,businessProfileIdBody,contentBody,placeIdBody
                ,reviewsBody,nameBody,streetBody, cityBody,stateBody,zipCodeBody,countryBody,latBody,lngBody,imageParts,videoParts,anonymousUserIDBody).execute()
        }

    }

    private fun getFileFromContentUri(context: Context, uri: Uri, fileName: String): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val file = File(context.cacheDir, fileName)
            file.outputStream().use { output ->
                inputStream.copyTo(output)
            }
            file
        } catch (e: Exception) {
            Log.e("CreatePostWorker", "Failed to get file from URI", e)
            null
        }
    }

    suspend fun getFeed(pageNumber: Int,documentLimit: Int,lat: Double,lng: Double): Response<FeedModal> {
        val accessToken = getAccessToken()
        if (accessToken.isEmpty()) {
            throw IllegalStateException("Access token is null or empty")
        }
        return withContext(Dispatchers.IO) {
            val call = Retrofit.apiService(context).create(Application::class.java)
            return@withContext call.getFeed(accessToken,pageNumber,documentLimit,lat,lng).execute()
        }
    }

    suspend fun getStories(pageNumber: Int,documentLimit: Int): Response<StoriesModal> {
        val accessToken = getAccessToken()
        if (accessToken.isEmpty()) {
            throw IllegalStateException("Access token is null or empty")
        }
        return withContext(Dispatchers.IO) {
            val call = Retrofit.apiService(context).create(Application::class.java)
            return@withContext call.getStories(accessToken,pageNumber,documentLimit).execute()
        }
    }

    suspend fun getSavedPost(pageNumber: Int,documentLimit: Int): Response<FeedModal> {
        val accessToken = getAccessToken()
        if (accessToken.isEmpty()) {
            throw IllegalStateException("Access token is null or empty")
        }
        return withContext(Dispatchers.IO) {
            val call = Retrofit.apiService(context).create(Application::class.java)
            return@withContext call.getSavedPost(accessToken,pageNumber,documentLimit).execute()
        }
    }

    suspend fun createEvent(name: String, startDate: String, startTime: String, endDate: String, endTime: String, type: String, venueName: String, streamingLink: String
                            , description: String,placeName: String,lat: String,lng: String, imageFile: File
    ): Response<CreateEventModal> {
        val accessToken = getAccessToken()
        if (accessToken.isEmpty()) {
            throw IllegalStateException("Access token is null or empty")
        }
        // Convert strings to RequestBody
        val accessTokenBody = accessToken.toRequestBody("text/plain".toMediaTypeOrNull())
        val nameBody = name.toRequestBody("text/plain".toMediaTypeOrNull())
        val startDateBody = startDate.toRequestBody("text/plain".toMediaTypeOrNull())
        val startTimeBody = startTime.toRequestBody("text/plain".toMediaTypeOrNull())
        val endDateBody = endDate.toRequestBody("text/plain".toMediaTypeOrNull())
        val endTimeBody = endTime.toRequestBody("text/plain".toMediaTypeOrNull())
        val typeBody = type.toRequestBody("text/plain".toMediaTypeOrNull())
        val venueNameBody = venueName.toRequestBody("text/plain".toMediaTypeOrNull())
        val streamingLinkBody = streamingLink.toRequestBody("text/plain".toMediaTypeOrNull())
        val descriptionBody = description.toRequestBody("text/plain".toMediaTypeOrNull())
        val placeNameBody = placeName.toRequestBody("text/plain".toMediaTypeOrNull())
        val latBody = lat.toRequestBody("text/plain".toMediaTypeOrNull())
        val lngBody = lng.toRequestBody("text/plain".toMediaTypeOrNull())
        // Get the file extension
        val extension = MimeTypeMap.getFileExtensionFromUrl(imageFile.toURI().toString())
        // Get MIME type from the file extension
        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "image/*"
        // Convert file to MultipartBody.Part with dynamic mimeType
        val requestFile = imageFile.asRequestBody(mimeType.toMediaTypeOrNull())
        val imageBody = MultipartBody.Part.createFormData("images", imageFile.name, requestFile)
        // Make the API call
        return withContext(Dispatchers.IO) {
            val apiService = Retrofit.apiService(context).create(Application::class.java)
            apiService.createEvent(
                accessTokenBody, nameBody, startDateBody, startTimeBody,endDateBody,endTimeBody, typeBody, venueNameBody, streamingLinkBody, descriptionBody,placeNameBody,latBody,lngBody, imageBody
            ).execute()
        }
    }

    suspend fun createStory(
        imageFile: File?,
        videoFile: File?,
        taggedList: List<String>,
        placeName: String?,
        lat: Double?,
        lng: Double?,
        locationX: Float? = null,
        locationY: Float? = null,
        userTaggedId: String? = null,
        userTaggedName: String? = null,
        userTaggedX: Float? = null,
        userTaggedY: Float? = null
    ): Response<CreateStoryModal> {
        val accessToken = getAccessToken()
        if (accessToken.isEmpty()) {
            throw IllegalStateException("Access token is null or empty")
        }
        // Convert access token to RequestBody
        val accessTokenBody = accessToken.toRequestBody("text/plain".toMediaTypeOrNull())
        val imagePart = if (imageFile != null && imageFile.exists()) {
            val imageExtension = MimeTypeMap.getFileExtensionFromUrl(imageFile.toURI().toString())
            val imageMimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(imageExtension) ?: "image/*"
            val imageRequestFile = imageFile.asRequestBody(imageMimeType.toMediaTypeOrNull())
            MultipartBody.Part.createFormData("images", imageFile.name, imageRequestFile)
        } else {
            null
        }
        val videoPart = if (videoFile != null && videoFile.exists()) {
            val videoExtension = MimeTypeMap.getFileExtensionFromUrl(videoFile.toURI().toString())
            val videoMimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(videoExtension) ?: "video/*"
            val videoRequestFile = videoFile.asRequestBody(videoMimeType.toMediaTypeOrNull())
            MultipartBody.Part.createFormData("videos", videoFile.name, videoRequestFile)
        } else {
            null
        }
        // Convert tagged list to MultipartBody.Part similar to createPost
        val taggedParts = taggedList.map { tagId ->
            MultipartBody.Part.createFormData("tagged[]", tagId)
        }
        // Convert location data to RequestBody
        val placeNameBody = placeName?.toRequestBody("text/plain".toMediaTypeOrNull())
        val latBody = lat?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())
        val lngBody = lng?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())
        val locationPositionXBody = locationX?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())
        val locationPositionYBody = locationY?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())
        val userTaggedBody = userTaggedName?.toRequestBody("text/plain".toMediaTypeOrNull())
        val userTaggedIdBody = userTaggedId?.toRequestBody("text/plain".toMediaTypeOrNull())
        val userTaggedXBody = userTaggedX?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())
        val userTaggedYBody = userTaggedY?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())
        
        android.util.Log.d("IndividualRepo", "Creating story with location - placeName: $placeName, lat: $lat, lng: $lng, locationPositionX: $locationX, locationPositionY: $locationY")
        android.util.Log.d("IndividualRepo", "Creating story with user tag - userTaggedId: $userTaggedId, userTaggedName: $userTaggedName, userTaggedX: $userTaggedX, userTaggedY: $userTaggedY")
        
        // Make the API call
        return withContext(Dispatchers.IO) {
            val apiService = Retrofit.apiService(context).create(Application::class.java)
            apiService.createStory(
                accessTokenBody, taggedParts, imagePart, videoPart, placeNameBody, latBody, lngBody, locationPositionXBody, locationPositionYBody, userTaggedBody, userTaggedIdBody, userTaggedXBody, userTaggedYBody
            ).execute()
        }
    }

    suspend fun publishPostToStory(postId: String): Response<PublishStoryModal> {
        val accessToken = getAccessToken()
        if (accessToken.isEmpty()) {
            throw IllegalStateException("Access token is null or empty")
        }
        val bearerToken = "Bearer $accessToken"
        return withContext(Dispatchers.IO) {
            val call = Retrofit.apiService(context).create(Application::class.java)
            return@withContext call.publishPostToStory(
                bearerToken,
                postId,
                emptyMap()
            ).execute()
        }
    }

    suspend fun savePost(id: String): Response<SavePostModal> {
        val accessToken = getAccessToken()
        if (accessToken.isEmpty()) {
            throw IllegalStateException("Access token is null or empty")
        }
        return withContext(Dispatchers.IO) {
            val call = Retrofit.apiService(context).create(Application::class.java)
            return@withContext call.savePost(accessToken,id).execute()
        }
    }

    suspend fun likePost(id: String): Response<SavePostModal> {
        val accessToken = getAccessToken()
        if (accessToken.isEmpty()) {
            throw IllegalStateException("Access token is null or empty")
        }
        return withContext(Dispatchers.IO) {
            val call = Retrofit.apiService(context).create(Application::class.java)
            return@withContext call.likePost(accessToken,id).execute()
        }
    }

    suspend fun joinEvent(id: String): Response<SavePostModal> {
        val accessToken = getAccessToken()
        if (accessToken.isEmpty()) {
            throw IllegalStateException("Access token is null or empty")
        }
        return withContext(Dispatchers.IO) {
            val call = Retrofit.apiService(context).create(Application::class.java)
            return@withContext call.joinEvent(accessToken,id).execute()
        }
    }

    suspend fun getComments(id: String,pageNumber: Int,documentLimit: Int): Response<GetCommentModal> {
        val accessToken = getAccessToken()
        if (accessToken.isEmpty()) {
            throw IllegalStateException("Access token is null or empty")
        }
        return withContext(Dispatchers.IO) {
            val call = Retrofit.apiService(context).create(Application::class.java)
            return@withContext call.getComments(accessToken,id,pageNumber,documentLimit).execute()
        }
    }

    suspend fun createComment(postID: String,message: String,parentID: String): Response<CreateCommentModal> {
        val accessToken = getAccessToken()
        if (accessToken.isEmpty()) {
            throw IllegalStateException("Access token is null or empty")
        }
        return withContext(Dispatchers.IO) {
            val call = Retrofit.apiService(context).create(Application::class.java)
            return@withContext call.createComments(accessToken,postID,message,parentID).execute()
        }
    }

    suspend fun likeComments(id: String): Response<SavePostModal> {
        val accessToken = getAccessToken()
        if (accessToken.isEmpty()) {
            throw IllegalStateException("Access token is null or empty")
        }
        return withContext(Dispatchers.IO) {
            val call = Retrofit.apiService(context).create(Application::class.java)
            return@withContext call.likeComments(accessToken,id).execute()
        }
    }

    suspend fun deleteComment(commentId: String): Response<DeleteModal> {
        val accessToken = getAccessToken()
        if (accessToken.isEmpty()) {
            throw IllegalStateException("Access token is null or empty")
        }
        return withContext(Dispatchers.IO) {
            val call = Retrofit.apiService(context).create(Application::class.java)
            return@withContext call.deleteComment(accessToken,commentId).execute()
        }
    }

    suspend fun getVideos(id: String,pageNumber: Int,documentLimit: Int): Response<VideoModal> {
        val accessToken = getAccessToken()
        if (accessToken.isEmpty()) {
            throw IllegalStateException("Access token is null or empty")
        }
        return withContext(Dispatchers.IO) {
            val call = Retrofit.apiService(context).create(Application::class.java)
            return@withContext call.getVideos(accessToken,id,pageNumber,documentLimit).execute()
        }
    }

    suspend fun getImage(id: String,pageNumber: Int,documentLimit: Int): Response<ImageModal> {
        val accessToken = getAccessToken()
        if (accessToken.isEmpty()) {
            throw IllegalStateException("Access token is null or empty")
        }
        return withContext(Dispatchers.IO) {
            val call = Retrofit.apiService(context).create(Application::class.java)
            return@withContext call.getImage(accessToken,id,pageNumber,documentLimit).execute()
        }
    }

    suspend fun getTaggedPeople(search: String,pageNumber: Int,documentLimit: Int): Response<TaggedPeopleModal> {
        val accessToken = getAccessToken()
        if (accessToken.isEmpty()) {
            throw IllegalStateException("Access token is null or empty")
        }
        return withContext(Dispatchers.IO) {
            val call = Retrofit.apiService(context).create(Application::class.java)
            return@withContext call.getTaggedPeople(accessToken,search,pageNumber,documentLimit).execute()
        }
    }

    suspend fun getUserProfileById(userId: String): Response<UserProfileModel> {
        val accessToken = getAccessToken()
        if (accessToken.isEmpty()) {
            throw IllegalStateException("Access token is null or empty")
        }
        return withContext(Dispatchers.IO) {
            val call = Retrofit.apiService(context).create(Application::class.java)
            return@withContext call.getUserProfileById(accessToken,userId).execute()
        }
    }

    suspend fun getBusinessProfileById(businessProfileId: String): Response<UserProfileModel> {
        return withContext(Dispatchers.IO) {
            // Use authApiService since this endpoint doesn't require authentication
            val call = Retrofit.authApiService(context).create(Application::class.java)
            return@withContext call.getBusinessProfileById(businessProfileId).execute()
        }
    }

    suspend fun getNotification(pageNumber: Int,documentLimit: Int): Response<NotificationModal> {
        val accessToken = getAccessToken()
        if (accessToken.isEmpty()) {
            throw IllegalStateException("Access token is null or empty")
        }
        return withContext(Dispatchers.IO) {
            val call = Retrofit.apiService(context).create(Application::class.java)
            return@withContext call.getNotifications(accessToken,pageNumber,documentLimit).execute()
        }
    }

    suspend fun getSuggestions(pageNumber: Int,documentLimit: Int): Response<SuggestedBusinessModal> {
        val accessToken = getAccessToken()
        if (accessToken.isEmpty()) {
            throw IllegalStateException("Access token is null or empty")
        }
        return withContext(Dispatchers.IO) {
            val call = Retrofit.apiService(context).create(Application::class.java)
            return@withContext call.getSuggestions(accessToken,pageNumber,documentLimit).execute()
        }
    }

    suspend fun getFollowerData(userId: String,pageNumber: Int,documentLimit: Int): Response<FollowFollowingModal> {
        val accessToken = getAccessToken()
        if (accessToken.isEmpty()) {
            throw IllegalStateException("Access token is null or empty")
        }
        return withContext(Dispatchers.IO) {
            val call = Retrofit.apiService(context).create(Application::class.java)
            return@withContext call.getFollowerData(accessToken,userId,pageNumber,documentLimit).execute()
        }
    }

    suspend fun getBlockUser(pageNumber: Int,documentLimit: Int): Response<FollowFollowingModal> {
        val accessToken = getAccessToken()
        if (accessToken.isEmpty()) {
            throw IllegalStateException("Access token is null or empty")
        }
        return withContext(Dispatchers.IO) {
            val call = Retrofit.apiService(context).create(Application::class.java)
            return@withContext call.getBlockUser(accessToken,pageNumber,documentLimit).execute()
        }
    }

    suspend fun getFollowingData(userId: String,pageNumber: Int,documentLimit: Int): Response<FollowFollowingModal> {
        val accessToken = getAccessToken()
        if (accessToken.isEmpty()) {
            throw IllegalStateException("Access token is null or empty")
        }
        return withContext(Dispatchers.IO) {
            val call = Retrofit.apiService(context).create(Application::class.java)
            return@withContext call.getFollowingData(accessToken,userId,pageNumber,documentLimit).execute()
        }
    }

    suspend fun acceptRequest(connectionId: String): Response<FollowActionModal> {
        val accessToken = getAccessToken()
        if (accessToken.isEmpty()) {
            throw IllegalStateException("Access token is null or empty")
        }
        return withContext(Dispatchers.IO) {
            val call = Retrofit.apiService(context).create(Application::class.java)
            return@withContext call.acceptRequest(accessToken,connectionId,"Demo").execute()
        }
    }

    suspend fun declineRequest(connectionId: String): Response<FollowActionModal> {
        val accessToken = getAccessToken()
        if (accessToken.isEmpty()) {
            throw IllegalStateException("Access token is null or empty")
        }
        return withContext(Dispatchers.IO) {
            val call = Retrofit.apiService(context).create(Application::class.java)
            return@withContext call.declineRequest(accessToken,connectionId,"Demo").execute()
        }
    }

    suspend fun followBack(connectionId: String): Response<FollowActionModal> {
        val accessToken = getAccessToken()
        if (accessToken.isEmpty()) {
            throw IllegalStateException("Access token is null or empty")
        }
        return withContext(Dispatchers.IO) {
            val call = Retrofit.apiService(context).create(Application::class.java)
            return@withContext call.followBack(accessToken,connectionId,"Demo").execute()
        }
    }

    suspend fun followUser(userId: String): Response<FollowUserModal> {
        val accessToken = getAccessToken()
        if (accessToken.isEmpty()) {
            throw IllegalStateException("Access token is null or empty")
        }
        return withContext(Dispatchers.IO) {
            val call = Retrofit.apiService(context).create(Application::class.java)
            return@withContext call.followUser(accessToken,userId,"Demo").execute()
        }
    }

    suspend fun unFollowUser(userId: String): Response<UnfollowModel> {
        val accessToken = getAccessToken()
        if (accessToken.isEmpty()) {
            throw IllegalStateException("Access token is null or empty")
        }
        return withContext(Dispatchers.IO) {
            val call = Retrofit.apiService(context).create(Application::class.java)
            return@withContext call.unFollowUser(accessToken,userId).execute()
        }
    }

    suspend fun getSearchData(query: String,type: String,pageNumber: Int,documentLimit: Int,businessTypeID: List<String>,initialKm :String,lat :Double,lng :Double): Response<SearchModel> {
        val accessToken = getAccessToken()
        if (accessToken.isEmpty()) {
            throw IllegalStateException("Access token is null or empty")
        }
        return withContext(Dispatchers.IO) {
            val call = Retrofit.apiService(context).create(Application::class.java)
            return@withContext call.getSearchData(accessToken,query,type,pageNumber,documentLimit,businessTypeID,initialKm,lat,lng).execute()
        }
    }

    suspend fun getPostsData(userId: String,pageNumber: Int,documentLimit: Int): Response<FeedModal> {
        val accessToken = getAccessToken()
        if (accessToken.isEmpty()) {
            throw IllegalStateException("Access token is null or empty")
        }
        return withContext(Dispatchers.IO) {
            val call = Retrofit.apiService(context).create(Application::class.java)
            return@withContext call.getPostsData(accessToken,userId,pageNumber,documentLimit).execute()
        }
    }

    suspend fun getReviewData(userId: String,pageNumber: Int,documentLimit: Int): Response<SearchModel> {
        val accessToken = getAccessToken()
        if (accessToken.isEmpty()) {
            throw IllegalStateException("Access token is null or empty")
        }
        return withContext(Dispatchers.IO) {
            val call = Retrofit.apiService(context).create(Application::class.java)
            return@withContext call.getReviewData(accessToken,userId,pageNumber,documentLimit).execute()
        }
    }

    suspend fun getTransactionData(pageNumber: Int,documentLimit: Int): Response<TransactionModal> {
        val accessToken = getAccessToken()
        if (accessToken.isEmpty()) {
            throw IllegalStateException("Access token is null or empty")
        }
        return withContext(Dispatchers.IO) {
            val call = Retrofit.apiService(context).create(Application::class.java)
            return@withContext call.getTransactionData(accessToken,pageNumber,documentLimit).execute()
        }
    }

    suspend fun getFaq(query: String,type: String,pageNumber: Int,documentLimit: Int): Response<FAQsModal> {
//        val accessToken = getAccessToken()
//        if (accessToken.isEmpty()) {
//            throw IllegalStateException("Access token is null or empty")
//        }
        return withContext(Dispatchers.IO) {
            val call = Retrofit.authApiService(context).create(Application::class.java)
            return@withContext call.getFaq(query,type,pageNumber,documentLimit).execute()
        }
    }

    suspend fun contactUs(name: String,email: String,message: String): Response<ContactUsModal> {
//        val accessToken = getAccessToken()
//        if (accessToken.isEmpty()) {
//            throw IllegalStateException("Access token is null or empty")
//        }
        return withContext(Dispatchers.IO) {
            val call = Retrofit.authApiService(context).create(Application::class.java)
            return@withContext call.contactUs(name,email,message).execute()
        }
    }

    suspend fun getSubscriptionsData(): Response<SubscriptionsModal> {
        val accessToken = getAccessToken()
        if (accessToken.isEmpty()) {
            throw IllegalStateException("Access token is null or empty")
        }
        return withContext(Dispatchers.IO) {
            val call = Retrofit.apiService(context).create(Application::class.java)
            return@withContext call.getSubscriptionsData(accessToken).execute()
        }
    }


    suspend fun cancelSubscriptions(): Response<CancelSubscriptions> {
        val accessToken = getAccessToken()
        if (accessToken.isEmpty()) {
            throw IllegalStateException("Access token is null or empty")
        }
        return withContext(Dispatchers.IO) {
            val call = Retrofit.apiService(context).create(Application::class.java)
            return@withContext call.cancelSubscription(accessToken).execute()
        }
    }

    suspend fun getAllPosts(id: String): Response<ViewPostEventModal> {
        val accessToken = getAccessToken()
        if (accessToken.isEmpty()) {
            throw IllegalStateException("Access token is null or empty")
        }
        return withContext(Dispatchers.IO) {
            val call = Retrofit.apiService(context).create(Application::class.java)
            return@withContext call.getAllPosts(accessToken,id).execute()
        }
    }
    suspend fun getSinglePosts(id: String): Response<SharePostModal> {
        val accessToken = getAccessToken()
        if (accessToken.isEmpty()) {
            throw IllegalStateException("Access token is null or empty")
        }
        return withContext(Dispatchers.IO) {
            val call = Retrofit.apiService(context).create(Application::class.java)
            return@withContext call.getSinglePosts(accessToken,id).execute()
        }
    }

    suspend fun visitWebsite(businessProfileID: String): Response<WebsiteRedirectModal> {
        val accessToken = getAccessToken()
        if (accessToken.isEmpty()) {
            throw IllegalStateException("Access token is null or empty")
        }
        return withContext(Dispatchers.IO) {
            val call = Retrofit.apiService(context).create(Application::class.java)
            return@withContext call.visitWebsite(accessToken,"website-redirection",businessProfileID).execute()
        }
    }
    suspend fun blockUser(id: String): Response<BlockUserModel> {
        val accessToken = getAccessToken()
        if (accessToken.isEmpty()) {
            throw IllegalStateException("Access token is null or empty")
        }
        return withContext(Dispatchers.IO) {
            val call = Retrofit.apiService(context).create(Application::class.java)
            return@withContext call.blockUser(accessToken,id,"demo").execute()
        }
    }



    suspend fun getInsight(filter : String): Response<InsightModal> {
        val accessToken = getAccessToken()
        if (accessToken.isEmpty()) {
            throw IllegalStateException("Access token is null or empty")
        }
        return withContext(Dispatchers.IO) {
            val call = Retrofit.apiService(context).create(Application::class.java)
            return@withContext call.insight(accessToken,filter).execute()
        }
    }

    suspend fun accountReach(businessProfileID: String): Response<AccountReachModal> {
        val accessToken = getAccessToken()
        if (accessToken.isEmpty()) {
            throw IllegalStateException("Access token is null or empty")
        }
        return withContext(Dispatchers.IO) {
            val call = Retrofit.apiService(context).create(Application::class.java)
            return@withContext call.accountReach(accessToken,"account-reach",businessProfileID).execute()
        }
    }


    suspend fun updateAddress(street: String,city: String,state: String,zipCode: String,country: String,lat: String,lng: String): Response<UpdateAddressModal> {
        val accessToken = getAccessToken()
        if (accessToken.isEmpty()) {
            throw IllegalStateException("Access token is null or empty")
        }
        return withContext(Dispatchers.IO) {
            val call = Retrofit.apiService(context).create(Application::class.java)
            return@withContext call.updateAddress(accessToken,street,city,state,zipCode,country,lat,lng).execute()
        }
    }

    suspend fun deleteStory(storyId: String): Response<DeleteStoryModal> {
        val accessToken = getAccessToken()
        if (accessToken.isEmpty()) {
            throw IllegalStateException("Access token is null or empty")
        }
        return withContext(Dispatchers.IO) {
            val call = Retrofit.apiService(context).create(Application::class.java)
            return@withContext call.deleteStory(accessToken,storyId).execute()
        }
    }

    suspend fun viewStory(storyId: String): Response<StoryActionModal> {
        val accessToken = getAccessToken()
        if (accessToken.isEmpty()) {
            throw IllegalStateException("Access token is null or empty")
        }
        return withContext(Dispatchers.IO) {
            val call = Retrofit.apiService(context).create(Application::class.java)
            return@withContext call.viewStory(accessToken,storyId,"Demo").execute()
        }
    }

    suspend fun likeStory(storyId: String): Response<StoryActionModal> {
        val accessToken = getAccessToken()
        if (accessToken.isEmpty()) {
            throw IllegalStateException("Access token is null or empty")
        }
        return withContext(Dispatchers.IO) {
            val call = Retrofit.apiService(context).create(Application::class.java)
            return@withContext call.likeStory(accessToken,storyId,"Demo").execute()
        }
    }



    suspend fun getLikes(storyId: String,pageNumber: Int,documentLimit: Int): Response<LikesModal> {
        val accessToken = getAccessToken()
        if (accessToken.isEmpty()) {
            throw IllegalStateException("Access token is null or empty")
        }
        return withContext(Dispatchers.IO) {
            val call = Retrofit.apiService(context).create(Application::class.java)
            return@withContext call.getLikes(accessToken,storyId,pageNumber,documentLimit).execute()
        }
    }

    suspend fun getViewers(storyId: String,pageNumber: Int,documentLimit: Int): Response<LikesModal> {
        val accessToken = getAccessToken()
        if (accessToken.isEmpty()) {
            throw IllegalStateException("Access token is null or empty")
        }
        return withContext(Dispatchers.IO) {
            val call = Retrofit.apiService(context).create(Application::class.java)
            return@withContext call.getViewers(accessToken,storyId,pageNumber,documentLimit).execute()
        }
    }


    suspend fun sendMediaMessage(username: String, message: String, messageType: String, media: File): Response<SendMediaModal> {

        println("dsjaklj    username   $username")
        println("dsjaklj    message   $message")
        println("dsjaklj    messageType   $messageType")
        println("dsjaklj    media   $media")

        val accessToken = getAccessToken()
        if (accessToken.isEmpty()) {
            throw IllegalStateException("Access token is null or empty")
        }

        // Convert strings to RequestBody
        val accessTokenBody = accessToken.toRequestBody("text/plain".toMediaTypeOrNull())
        val userNameBody = username.toRequestBody("text/plain".toMediaTypeOrNull())
        val messageBody = message.toRequestBody("text/plain".toMediaTypeOrNull())
        val messageTypeBody = messageType.toRequestBody("text/plain".toMediaTypeOrNull())

        // Ensure mediaPart is non-null by providing a default value if needed
        val mediaPart: MultipartBody.Part = if (messageType == "image") {
            val imageExtension = MimeTypeMap.getFileExtensionFromUrl(media.toURI().toString())
            val imageMimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(imageExtension) ?: "image/*"
            val imageRequestFile = media.asRequestBody(imageMimeType.toMediaTypeOrNull())
            MultipartBody.Part.createFormData("media", media.name, imageRequestFile)
        } else if (messageType == "video") {
            val videoExtension = MimeTypeMap.getFileExtensionFromUrl(media.toURI().toString())
            val videoMimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(videoExtension) ?: "video/*"
            val videoRequestFile = media.asRequestBody(videoMimeType.toMediaTypeOrNull())
            MultipartBody.Part.createFormData("media", media.name, videoRequestFile)
        } else if (messageType == "pdf") {
            // For PDF, set the mime type dynamically
            val pdfMimeType = "application/pdf"
            val pdfRequestFile = media.asRequestBody(pdfMimeType.toMediaTypeOrNull())
            MultipartBody.Part.createFormData("media", media.name, pdfRequestFile)
        } else {
            throw IllegalArgumentException("Unsupported message type: $messageType")
        }



        // Make the API call
        return withContext(Dispatchers.IO) {
            val call = Retrofit.apiService(context).create(Application::class.java)
            return@withContext call.sendMediaMessage(accessTokenBody, userNameBody, messageBody, messageTypeBody, mediaPart).execute()
        }
    }

    /**
     * Helper function to download media file from URL to a temporary file
     */
    private suspend fun downloadMediaFile(mediaUrl: String, messageType: String): File {
        return withContext(Dispatchers.IO) {
            val url = java.net.URL(mediaUrl)
            val connection = url.openConnection() as java.net.HttpURLConnection
            connection.connect()

            val mimeType = connection.contentType ?: when (messageType) {
                "image" -> "image/jpeg"
                "video" -> "video/mp4"
                else -> "application/octet-stream"
            }
            
            val extension = when {
                messageType == "image" -> {
                    if (mimeType.contains("png")) "png" else "jpg"
                }
                messageType == "video" -> {
                    if (mediaUrl.contains(".m3u8")) {
                        // Handle HLS streams - this is complex, for now return a temp file
                        // In production, you might want to handle this differently
                        throw IllegalStateException("HLS streams need special handling")
                    } else "mp4"
                }
                else -> "bin"
            }

            val cacheDir = context.cacheDir
            val fileName = "share_post_media_${System.currentTimeMillis()}.$extension"
            val file = File(cacheDir, fileName)

            connection.inputStream.use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            file
        }
    }

    suspend fun sharePostMessage(
        username: String,
        messageType: String,
        message: String?,
        postID: String,
        mediaUrl: String
    ): Response<SendMediaModal> {
        val accessToken = getAccessToken()
        if (accessToken.isEmpty()) {
            throw IllegalStateException("Access token is null or empty")
        }

        return withContext(Dispatchers.IO) {
            // Download the media file from URL
            val mediaFile = downloadMediaFile(mediaUrl, messageType)

            try {
                // Convert strings to RequestBody
                val accessTokenBody = accessToken.toRequestBody("text/plain".toMediaTypeOrNull())
                val userNameBody = username.toRequestBody("text/plain".toMediaTypeOrNull())
                val messageTypeBody = messageType.toRequestBody("text/plain".toMediaTypeOrNull())
                val messageBody = (message ?: "").toRequestBody("text/plain".toMediaTypeOrNull())
                val postIDBody = postID.toRequestBody("text/plain".toMediaTypeOrNull())

                // Create media part
                val mediaPart: MultipartBody.Part = when (messageType) {
                    "image" -> {
                        val mimeType = "image/jpeg"
                        val requestFile = mediaFile.asRequestBody(mimeType.toMediaTypeOrNull())
                        MultipartBody.Part.createFormData("media", mediaFile.name, requestFile)
                    }
                    "video" -> {
                        val mimeType = "video/mp4"
                        val requestFile = mediaFile.asRequestBody(mimeType.toMediaTypeOrNull())
                        MultipartBody.Part.createFormData("media", mediaFile.name, requestFile)
                    }
                    else -> throw IllegalArgumentException("Unsupported message type: $messageType")
                }

                // Make the API call
                val call = Retrofit.apiService(context).create(Application::class.java)
                val response = call.sharePostMessage(
                    accessTokenBody,
                    userNameBody,
                    messageTypeBody,
                    messageBody,
                    postIDBody,
                    mediaPart
                ).execute()

                // Delete temporary file after upload
                mediaFile.delete()

                response
            } catch (e: Exception) {
                // Ensure file is deleted even on error
                if (mediaFile.exists()) {
                    mediaFile.delete()
                }
                throw e
            }
        }
    }


    suspend fun reportUser(userId: String,reason: String): Response<ReportUserModal> {
        val accessToken = getAccessToken()
        if (accessToken.isEmpty()) {
            throw IllegalStateException("Access token is null or empty")
        }
        return withContext(Dispatchers.IO) {
            val call = Retrofit.apiService(context).create(Application::class.java)
            return@withContext call.reportUser(accessToken,userId,reason).execute()
        }
    }

    suspend fun reportPosts(postId: String,reason: String): Response<ReportUserModal> {
        val accessToken = getAccessToken()
        if (accessToken.isEmpty()) {
            throw IllegalStateException("Access token is null or empty")
        }
        return withContext(Dispatchers.IO) {
            val call = Retrofit.apiService(context).create(Application::class.java)
            return@withContext call.reportPost(accessToken,postId,reason).execute()
        }
    }

    suspend fun reportComment(commentId: String,reason: String): Response<ReportUserModal> {
        val accessToken = getAccessToken()
        if (accessToken.isEmpty()) {
            throw IllegalStateException("Access token is null or empty")
        }
        return withContext(Dispatchers.IO) {
            val call = Retrofit.apiService(context).create(Application::class.java)
            return@withContext call.reportComment(accessToken,commentId,reason).execute()
        }
    }

    suspend fun getSharedPosts(postId: String,userID: String): Response<SharePostModal> {
        val accessToken = getAccessToken()
        if (accessToken.isEmpty()) {
            throw IllegalStateException("Access token is null or empty")
        }
        return withContext(Dispatchers.IO) {
            val call = Retrofit.apiService(context).create(Application::class.java)
            return@withContext call.getSharedPosts(accessToken,postId,userID).execute()
        }
    }

    suspend fun getSavedDocuments(): Response<GetBusinessDocumentsModal> {
        val accessToken = getAccessToken()
        if (accessToken.isEmpty()) {
            throw IllegalStateException("Access token is null or empty")
        }
        return withContext(Dispatchers.IO) {
            val call = Retrofit.apiService(context).create(Application::class.java)
            return@withContext call.getSavedDocuments(accessToken).execute()
        }
    }

    suspend fun getNotificationStatus(): Response<NotificationStatusModal> {
        val accessToken = getAccessToken()
        if (accessToken.isEmpty()) {
            throw IllegalStateException("Access token is null or empty")
        }
        return withContext(Dispatchers.IO) {
            val call = Retrofit.apiService(context).create(Application::class.java)
            return@withContext call.getNotificationStatus(accessToken).execute()
        }
    }


    suspend fun bookingCheckIn(businessProfileID: String,checkInDate: String,checkOutDate: String,adultsCount: Int,childrenCount: Int,childrenAges: List<Int>,isTravellingWithPet : Boolean): Response<BookingCheckInModal> {
        val accessToken = getAccessToken()
        if (accessToken.isEmpty()) {
            throw IllegalStateException("Access token is null or empty")
        }
        return withContext(Dispatchers.IO) {
            val call = Retrofit.apiService(context).create(Application::class.java)
            // Send both key variants for compatibility with differing backend field naming.
            return@withContext call.bookingCheckIn(
                accessToken,
                businessProfileID,
                businessProfileID,
                checkInDate,
                checkInDate,
                checkOutDate,
                checkOutDate,
                adultsCount,
                childrenCount,
                childrenAges,
                isTravellingWithPet
            ).execute()
        }
    }

    suspend fun fetchRoomDetails(roomId: String): Response<RoomDetailsModal> {
        val accessToken = getAccessToken()
        if (accessToken.isEmpty()) {
            throw IllegalStateException("Access token is null or empty")
        }
        return withContext(Dispatchers.IO) {
            val call = Retrofit.apiService(context).create(Application::class.java)
            return@withContext call.fetchRoomDetails(accessToken,roomId).execute()
        }
    }

    suspend fun getRoomsByBusinessProfile(businessProfileID: String): Response<com.thehotelmedia.android.modals.booking.roomsList.RoomsListModal> {
        val accessToken = getAccessToken()
        if (accessToken.isEmpty()) {
            throw IllegalStateException("Access token is null or empty")
        }
        return withContext(Dispatchers.IO) {
            val call = Retrofit.apiService(context).create(Application::class.java)
            return@withContext call.getRoomsByBusinessProfile(accessToken, businessProfileID, businessProfileID).execute()
        }
    }

    suspend fun bookingCheckOut(bookingId: String,roomId: String,quantity: Int,promoCode: String,price: Double): Response<BookingCheckOutModal> {
        val accessToken = getAccessToken()
        if (accessToken.isEmpty()) {
            throw IllegalStateException("Access token is null or empty")
        }
        return withContext(Dispatchers.IO) {
            val call = Retrofit.apiService(context).create(Application::class.java)
            return@withContext call.bookingCheckOut(accessToken,bookingId,roomId,quantity,"myself",promoCode,price).execute()
        }
    }

    suspend fun bookRoom(
        bookingId: String,
        paymentID: String,
        signature: String,
        guestDetails: List<String>,
        selectedGuest: String
    ): Response<BookRoomModal> {
        val accessToken = getAccessToken()
        if (accessToken.isEmpty()) {
            throw IllegalStateException("Access token is null or empty")
        }
        return withContext(Dispatchers.IO) {
            val call = Retrofit.apiService(context).create(Application::class.java)
            return@withContext call.bookRoom(accessToken,bookingId,paymentID,signature,guestDetails,selectedGuest).execute()
        }
    }

    suspend fun getBookingHistory(pageNumber: Int,documentLimit: Int): Response<BookingHistoryModal> {
        val accessToken = getAccessToken()
        if (accessToken.isEmpty()) {
            throw IllegalStateException("Access token is null or empty")
        }
        return withContext(Dispatchers.IO) {
            val call = Retrofit.apiService(context).create(Application::class.java)
            return@withContext call.getBookingHistory(accessToken,pageNumber,documentLimit).execute()
        }
    }

    suspend fun getBookingSummary(bookingId: String): Response<BookingSummaryModal> {
        val accessToken = getAccessToken()
        if (accessToken.isEmpty()) {
            throw IllegalStateException("Access token is null or empty")
        }
        return withContext(Dispatchers.IO) {
            val call = Retrofit.apiService(context).create(Application::class.java)
            return@withContext call.getBookingSummary(accessToken,bookingId).execute()
        }
    }

    suspend fun acceptRejectTableBanquet(bookingId: String,status: String): Response<AcceptRejectBookingModal> {
        val accessToken = getAccessToken()
        if (accessToken.isEmpty()) {
            throw IllegalStateException("Access token is null or empty")
        }
        return withContext(Dispatchers.IO) {
            val call = Retrofit.apiService(context).create(Application::class.java)
            return@withContext call.acceptRejectTableBanquet(accessToken,bookingId,status).execute()
        }
    }

    suspend fun downloadBookingInvoice(bookingId: String): Response<ExportChatModal> {
        val accessToken = getAccessToken()
        if (accessToken.isEmpty()) {
            throw IllegalStateException("Access token is null or empty")
        }
        return withContext(Dispatchers.IO) {
            val call = Retrofit.apiService(context).create(Application::class.java)
            return@withContext call.downloadBookingInvoice(accessToken,bookingId).execute()
        }
    }
    suspend fun cancelBooking(bookingId: String): Response<DeleteModal> {
        val accessToken = getAccessToken()
        if (accessToken.isEmpty()) {
            throw IllegalStateException("Access token is null or empty")
        }
        return withContext(Dispatchers.IO) {
            val call = Retrofit.apiService(context).create(Application::class.java)
            return@withContext call.cancelBooking(accessToken,bookingId).execute()
        }
    }
    suspend fun sentOtpToNumber(dialCode: String,phoneNumber: String): Response<DeleteModal> {
        val accessToken = getAccessToken()
        if (accessToken.isEmpty()) {
            throw IllegalStateException("Access token is null or empty")
        }
        return withContext(Dispatchers.IO) {
            val call = Retrofit.apiService(context).create(Application::class.java)
            return@withContext call.sentOtpToNumber(accessToken,dialCode,phoneNumber).execute()
        }
    }
    suspend fun verifyNumberWithOtp(dialCode: String,phoneNumber: String,otp: String): Response<VerifyMobileNumberModal> {
        val accessToken = getAccessToken()
        if (accessToken.isEmpty()) {
            throw IllegalStateException("Access token is null or empty")
        }
        return withContext(Dispatchers.IO) {
            val call = Retrofit.apiService(context).create(Application::class.java)
            return@withContext call.verifyNumberWithOtp(accessToken,dialCode,phoneNumber,otp).execute()
        }
    }
    suspend fun bookATable(numberOfGuests: String,date: String,time: String,businessProfileID: String): Response<BookTableModal> {
        val accessToken = getAccessToken()
        if (accessToken.isEmpty()) {
            throw IllegalStateException("Access token is null or empty")
        }
        return withContext(Dispatchers.IO) {
            val call = Retrofit.apiService(context).create(Application::class.java)
            return@withContext call.bookATable(accessToken,numberOfGuests,date,time,businessProfileID).execute()
        }
    }
    suspend fun bookABanquet(checkIn: String,checkOut: String,businessProfileID: String,numberOfGuests: Int,typeOfEvent: String): Response<BookBanquetModal> {
        val accessToken = getAccessToken()
        if (accessToken.isEmpty()) {
            throw IllegalStateException("Access token is null or empty")
        }
        return withContext(Dispatchers.IO) {
            val call = Retrofit.apiService(context).create(Application::class.java)
            return@withContext call.bookABanquet(accessToken,checkIn,checkOut,businessProfileID,numberOfGuests,typeOfEvent).execute()
        }
    }
    suspend fun postJob(title: String,designation: String,description: String,jobType: String,salary: String,joiningDate: String,numberOfVacancies: String,experience: String): Response<PostJobModal> {
        val accessToken = getAccessToken()

        // Print all the input data
        println("Posting Job with the following details:")
        println("agdskgajkhgkja   Access Token: $accessToken")
        println("agdskgajkhgkja   Title: $title")
        println("agdskgajkhgkja   Designation: $designation")
        println("agdskgajkhgkja   Description: $description")
        println("agdskgajkhgkja   Job Type: $jobType")
        println("agdskgajkhgkja   Salary: $salary")
        println("agdskgajkhgkja   Joining Date: $joiningDate")
        println("agdskgajkhgkja   Number of Vacancies: $numberOfVacancies")
        println("agdskgajkhgkja   Experience: $experience")

        if (accessToken.isEmpty()) {
            throw IllegalStateException("Access token is null or empty")
        }
        return withContext(Dispatchers.IO) {
            val call = Retrofit.apiService(context).create(Application::class.java)
            return@withContext call.postJob(accessToken,title,designation,description,jobType,salary,joiningDate,numberOfVacancies,experience).execute()
        }
    }

    suspend fun getJobDetails(jobId: String): Response<JobDetailsModal> {
        val accessToken = getAccessToken()
        if (accessToken.isEmpty()) {
            throw IllegalStateException("Access token is null or empty")
        }
        return withContext(Dispatchers.IO) {
            val call = Retrofit.apiService(context).create(Application::class.java)
            return@withContext call.getJobDetails(accessToken,jobId).execute()
        }
    }

    // Collaboration
    suspend fun collaborationInvite(postID: String, invitedUserID: String): Response<CollaborationActionModal> {
        val accessToken = getAccessToken()
        if (accessToken.isEmpty()) {
            throw IllegalStateException("Access token is null or empty")
        }
        return withContext(Dispatchers.IO) {
            val call = Retrofit.apiService(context).create(Application::class.java)
            return@withContext call.collaborationInvite(accessToken, postID, invitedUserID).execute()
        }
    }

    suspend fun collaborationRespond(postID: String, action: String): Response<CollaborationActionModal> {
        val accessToken = getAccessToken()
        if (accessToken.isEmpty()) {
            throw IllegalStateException("Access token is null or empty")
        }
        return withContext(Dispatchers.IO) {
            val call = Retrofit.apiService(context).create(Application::class.java)
            return@withContext call.collaborationRespond(accessToken, postID, action).execute()
        }
    }

    suspend fun getCollaborationPosts(): Response<CollaborationPostsModal> {
        val accessToken = getAccessToken()
        if (accessToken.isEmpty()) {
            throw IllegalStateException("Access token is null or empty")
        }
        return withContext(Dispatchers.IO) {
            val call = Retrofit.apiService(context).create(Application::class.java)
            return@withContext call.getCollaborationPosts(accessToken).execute()
        }
    }

    suspend fun getPostCollaborators(postID: String): Response<CollaboratorsListModal> {
        val accessToken = getAccessToken()
        if (accessToken.isEmpty()) {
            throw IllegalStateException("Access token is null or empty")
        }
        return withContext(Dispatchers.IO) {
            val call = Retrofit.apiService(context).create(Application::class.java)
            return@withContext call.getPostCollaborators(accessToken, postID).execute()
        }
    }

    suspend fun getMenu(businessProfileId: String): Response<MenuResponse> {
        val accessToken = getAccessToken()
        if (accessToken.isEmpty()) {
            throw IllegalStateException("Access token is null or empty")
        }
        return withContext(Dispatchers.IO) {
            val call = Retrofit.apiService(context).create(Application::class.java)
            return@withContext call.getMenu(accessToken, businessProfileId).execute()
        }
    }

    suspend fun uploadMenu(businessProfileId: String, files: List<MultipartBody.Part>): Response<MenuResponse> {
        val accessToken = getAccessToken()
        if (accessToken.isEmpty()) {
            throw IllegalStateException("Access token is null or empty")
        }
        return withContext(Dispatchers.IO) {
            val call = Retrofit.apiService(context).create(Application::class.java)
            // Backend expects the multipart field name to be "menu"
            return@withContext call.uploadMenu(accessToken, files).execute()
        }
    }

    suspend fun deleteMenu(menuId: String): Response<MenuResponse> {
        val accessToken = getAccessToken()
        if (accessToken.isEmpty()) {
            throw IllegalStateException("Access token is null or empty")
        }
        return withContext(Dispatchers.IO) {
            val call = Retrofit.apiService(context).create(Application::class.java)
            return@withContext call.deleteMenu(accessToken, menuId).execute()
        }
    }

}