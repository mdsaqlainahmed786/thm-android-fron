package com.thehotelmedia.android.interFaces

import com.thehotelmedia.android.SocketModals.sendMedia.SendMediaModal
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
import com.thehotelmedia.android.modals.weatherOrAqi.aqi.AqiModal
import com.thehotelmedia.android.modals.weatherOrAqi.weather.WeatherModal
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.PUT
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query


interface Application {

    @GET("user/subscription/meta")
    fun getSubscriptionDetails(
        @Header("x-access-token") token: String,
    ): Call<SubscriptionData>

    @GET("weather")
    fun getWeather(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("appid") apiKey: String
    ): Call<WeatherModal>

    @GET("air_pollution")
    fun getAQI(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("appid") apiKey: String
    ): Call<AqiModal>

    @DELETE("user/messaging/chat"+"/{id}")
    fun deleteChat(
        @Header("x-access-token") token: String,
        @Path(value = "id",encoded = true) userId: String,
    ): Call<DeleteModal>

    @DELETE("posts"+"/{id}")
    fun deletePost(
        @Header("x-access-token") token: String,
        @Path(value = "id",encoded = true) postId: String,
    ): Call<DeleteModal>

    @POST("user/messaging/export-chat"+"/{id}")
    fun exportChat(
        @Header("x-access-token") token: String,
        @Path(value = "id",encoded = true) userId: String,
    ): Call<ExportChatModal>

    @POST("posts/media/views")
    @FormUrlEncoded
    fun viewMedia(
        @Header("x-access-token") token: String,
        @Field("postID") postID: String,
        @Field("mediaID") mediaID: String,
    ): Call<ViewMediaModal>

    @POST("posts/views")
    @FormUrlEncoded
    fun postViews(
        @Header("x-access-token") token: String,
        @Field("postIDs") businessTypeID: List<String>,
    ): Call<DeleteModal>

    @GET("share/users")
    fun shareProfileData(
        @Header("x-access-token") token: String,
        @Query("id") id: String,
        @Query("userID") userId: String
    ): Call<ShareProfileModal>

    @GET("user/profile")
    fun getProfile(
        @Header("x-access-token") token: String
    ): Call<GetProfileModal>

    @PATCH("user/edit-profile")
    @FormUrlEncoded
    fun updateLanguage(
        @Header("x-access-token") token: String,
        @Field("language") language: String,
    ): Call<EditProfileModal>

    @PATCH("user/edit-profile")
    @FormUrlEncoded
    fun editProfile(
        @Header("x-access-token") token: String,
        @Field("name") name: String,
        @Field("email") email: String,
        @Field("dialCode") dialCode: String,
        @Field("phoneNumber") phoneNumber: String,
        @Field("bio") bio: String,
    ): Call<EditProfileModal>

    @PATCH("user/edit-profile")
    @FormUrlEncoded
    fun updateStatus(
        @Header("x-access-token") token: String,
        @Field("privateAccount") privateAccount: Boolean,
        @Field("notificationEnabled") notificationEnabled: Boolean
    ): Call<EditProfileModal>

    @PATCH("user/edit-profile")
    @FormUrlEncoded
    fun editCategory(
        @Header("x-access-token") token: String,
        @Field("businessTypeID") businessTypeID: String,
        @Field("businessSubTypeID") businessSubTypeID: String
    ): Call<EditProfileModal>

    @GET("place/nearbysearch/json")
    suspend fun getNearbyPlaces(
        @Query("location") location: String, // Combine latitude and longitude in the parameter
        @Query("radius") radius: Int,
        @Query("type") type: String,
        @Query("key") key: String
    ): Response<NearByPlacesModel>

    @GET("business/{id}")
    fun getBusinessProfileById(
        @Path(value = "id",encoded = true) id: String
    ): Call<UserProfileModel>

    @GET("business/get-by-place"+"/{id}")
    fun getCheckInData(
        @Header("x-access-token") token: String,
        @Path(value = "id",encoded = true) placeId: String,
        @Query("businessProfileID") businessProfileID: String
    ): Call<CheckInDataModel>

    @Multipart
    @POST("posts")
    fun createPost(
        @Header("x-access-token") token: RequestBody,
        @Part("content") content: RequestBody,
        @Part("placeName") placeName: RequestBody,
        @Part("lat") lat: RequestBody,
        @Part("lng") lng: RequestBody,
        @Part("feelings") feelings: RequestBody,
        @Part tagged: List<MultipartBody.Part>,
        @Part media: List<MultipartBody.Part>  // List for videos
    ): Call<CreatePostModal>

    @Multipart
    @POST("reviews")
    fun createReview(
        @Header("x-access-token") token: RequestBody,
        @Part("businessProfileID") businessProfileID: RequestBody,
        @Part("content") content: RequestBody,
        @Part("placeID") placeID: RequestBody,
        @Part reviews: List<MultipartBody.Part>,
        @Part("name") name: RequestBody,
        @Part("street") street: RequestBody,
        @Part("city") city: RequestBody,
        @Part("state") state: RequestBody,
        @Part("zipCode") zipCode: RequestBody,
        @Part("country") country: RequestBody,
        @Part("lat") lat: RequestBody,
        @Part("lng") lng: RequestBody,
        @Part images: List<MultipartBody.Part>, // List for images
        @Part videos: List<MultipartBody.Part>,  // List for videos
        @Part("anonymousUserID") anonymousUserID: RequestBody,
    ): Call<CreateReviewModel>

    @GET("feed")
    fun getFeed(
        @Header("x-access-token") token: String,
        @Query("pageNumber") pageNumber: Int,
        @Query("documentLimit") documentLimit: Int,
        @Query("lat") lat: Double,
        @Query("lng") lng: Double,
    ): Call<FeedModal>

    @GET("story")
    fun getStories(
        @Header("x-access-token") token: String,
        @Query("pageNumber") pageNumber: Int,
        @Query("documentLimit") documentLimit: Int,
    ): Call<StoriesModal>

    @GET("posts/saved-posts")
    fun getSavedPost(
        @Header("x-access-token") token: String,
        @Query("pageNumber") pageNumber: Int,
        @Query("documentLimit") documentLimit: Int,
    ): Call<FeedModal>

    @Multipart
    @POST("events")
    fun createEvent(
        @Header("x-access-token") token: RequestBody,
        @Part("name") name: RequestBody,
        @Part("startDate") startDate: RequestBody,
        @Part("startTime") startTime: RequestBody,
        @Part("endDate") endDate: RequestBody,
        @Part("endTime") endTime: RequestBody,
        @Part("type") type: RequestBody,
        @Part("venue") venueName: RequestBody,
        @Part("streamingLink") streamingLink: RequestBody,
        @Part("description") description: RequestBody,
        @Part("placeName") placeName: RequestBody,
        @Part("lat") lat: RequestBody,
        @Part("lng") lng: RequestBody,
        @Part imageFile: MultipartBody.Part
    ): Call<CreateEventModal>

    @Multipart
    @POST("story")
    fun createStory(
        @Header("x-access-token") token: RequestBody,
        @Part tagged: List<MultipartBody.Part>,
        @Part imageFile: MultipartBody.Part?,
        @Part videoFile: MultipartBody.Part?
    ): Call<CreateStoryModal>

    @POST("posts/saved-posts"+"/{id}")
    fun savePost(
        @Header("x-access-token") token: String,
        @Path(value = "id",encoded = true) id: String,
    ): Call<SavePostModal>

    @POST("posts/likes"+"/{id}")
    fun likePost(
        @Header("x-access-token") token: String,
        @Path(value = "id",encoded = true) id: String,
    ): Call<SavePostModal>

    @POST("events/join")
    @FormUrlEncoded
    fun joinEvent(
        @Header("x-access-token") token: String,
        @Field("postID") postID: String,
    ): Call<SavePostModal>

    @GET("posts/comments"+"/{id}")
    fun getComments(
        @Header("x-access-token") token: String,
        @Path(value = "id",encoded = true) id: String,
        @Query("pageNumber") pageNumber: Int,
        @Query("documentLimit") documentLimit: Int,
    ): Call<GetCommentModal>

    @POST("posts/comments")
    @FormUrlEncoded
    fun createComments(
        @Header("x-access-token") token: String,
        @Field("postID") postID: String,
        @Field("message") message: String,
        @Field("parentID") parentID: String,
    ): Call<CreateCommentModal>

    @Multipart
    @PUT("posts"+"/{id}")
    fun updatePost(
        @Header("x-access-token") token: String,
        @Path(value = "id",encoded = true) id: String,
        @Part("content") content: RequestBody,
        @Part("feelings") feelings: RequestBody,
        @Part("deletedMedia") deletedMedia: RequestBody?,
        @Part media: List<MultipartBody.Part>,
        @Part("placeName") placeName: RequestBody?,
        @Part("lat") lat: RequestBody?,
        @Part("lng") lng: RequestBody?
    ): Call<DeleteModal>

    @POST("posts/comments/likes"+"/{id}")
    fun likeComments(
        @Header("x-access-token") token: String,
        @Path(value = "id",encoded = true) id: String,
    ): Call<SavePostModal>

    @DELETE("posts/comments"+"/{id}")
    fun deleteComment(
        @Header("x-access-token") token: String,
        @Path(value = "id",encoded = true) id: String,
    ): Call<DeleteModal>

    @GET("user/videos"+"/{id}")
    fun getVideos(
        @Header("x-access-token") token: String,
        @Path(value = "id",encoded = true) id: String,
        @Query("pageNumber") pageNumber: Int,
        @Query("documentLimit") documentLimit: Int,
    ): Call<VideoModal>

    @GET("user/images"+"/{id}")
    fun getImage(
        @Header("x-access-token") token: String,
        @Path(value = "id",encoded = true) id: String,
        @Query("pageNumber") pageNumber: Int,
        @Query("documentLimit") documentLimit: Int,
    ): Call<ImageModal>

    @GET("user/tag-people")
    fun getTaggedPeople(
        @Header("x-access-token") token: String,
        @Query("query") search: String,
        @Query("pageNumber") pageNumber: Int,
        @Query("documentLimit") documentLimit: Int,
    ): Call<TaggedPeopleModal>

    @GET("user/profile"+"/{id}")
    fun getUserProfileById(
        @Header("x-access-token") token: String,
        @Path(value = "id",encoded = true) id: String,
    ): Call<UserProfileModel>

    @GET("notifications")
    fun getNotifications(
        @Header("x-access-token") token: String,
        @Query("pageNumber") pageNumber: Int,
        @Query("documentLimit") documentLimit: Int,
    ): Call<NotificationModal>

    @GET("suggestions")
    fun getSuggestions(
        @Header("x-access-token") token: String,
        @Query("pageNumber") pageNumber: Int,
        @Query("documentLimit") documentLimit: Int,
    ): Call<SuggestedBusinessModal>

    @GET("user/follower"+"/{id}")
    fun getFollowerData(
        @Header("x-access-token") token: String,
        @Path(value = "id",encoded = true) userId: String,
        @Query("pageNumber") pageNumber: Int,
        @Query("documentLimit") documentLimit: Int,
    ): Call<FollowFollowingModal>

    @GET("user/blocks")
    fun getBlockUser(
        @Header("x-access-token") token: String,
        @Query("pageNumber") pageNumber: Int,
        @Query("documentLimit") documentLimit: Int,
    ): Call<FollowFollowingModal>

    @GET("user/following"+"/{id}")
    fun getFollowingData(
        @Header("x-access-token") token: String,
        @Path(value = "id",encoded = true) userId: String,
        @Query("pageNumber") pageNumber: Int,
        @Query("documentLimit") documentLimit: Int,
    ): Call<FollowFollowingModal>

    @POST("user/accept-follow"+"/{id}")
    @FormUrlEncoded
    fun acceptRequest(
        @Header("x-access-token") token: String,
        @Path(value = "id",encoded = true) connectionId: String,
        @Field("demo") demo: String,
    ): Call<FollowActionModal>

    @POST("user/reject-follow"+"/{id}")
    @FormUrlEncoded
    fun declineRequest(
        @Header("x-access-token") token: String,
        @Path(value = "id",encoded = true) connectionId: String,
        @Field("demo") demo: String,
    ): Call<FollowActionModal>

    @POST("user/follow-back"+"/{id}")
    @FormUrlEncoded
    fun followBack(
        @Header("x-access-token") token: String,
        @Path(value = "id",encoded = true) connectionId: String,
        @Field("demo") demo: String,
    ): Call<FollowActionModal>

    @POST("user/follow"+"/{id}")
    @FormUrlEncoded
    fun followUser(
        @Header("x-access-token") token: String,
        @Path(value = "id",encoded = true) userId: String,
        @Field("demo") demo: String,
    ): Call<FollowUserModal>

    @DELETE("user/unfollow" + "/{id}")
    fun unFollowUser(
        @Header("x-access-token") token: String,
        @Path(value = "id", encoded = true) userId: String
    ): Call<UnfollowModel>

    @GET("search")
    fun getSearchData(
        @Header("x-access-token") token: String,
        @Query("query") query: String,
        @Query("type") type: String,
        @Query("pageNumber") pageNumber: Int,
        @Query("documentLimit") documentLimit: Int,
        @Query("businessTypeID") businessTypeID: List<String>,
        @Query("radius") radius: String,
        @Query("lat") lat: Double,
        @Query("lng") lng: Double,
    ): Call<SearchModel>

    @GET("user/posts" + "/{id}")
    fun getPostsData(
        @Header("x-access-token") token: String,
        @Path(value = "id", encoded = true) userId: String,
        @Query("pageNumber") pageNumber: Int,
        @Query("documentLimit") documentLimit: Int,
    ): Call<FeedModal>

    @GET("user/reviews" + "/{id}")
    fun getReviewData(
        @Header("x-access-token") token: String,
        @Path(value = "id", encoded = true) userId: String,
        @Query("pageNumber") pageNumber: Int,
        @Query("documentLimit") documentLimit: Int,
    ): Call<SearchModel>

    @GET("transactions")
    fun getTransactionData(
        @Header("x-access-token") token: String,
        @Query("pageNumber") pageNumber: Int,
        @Query("documentLimit") documentLimit: Int,
    ): Call<TransactionModal>

    @GET("faqs")
    fun getFaq(
//        @Header("x-access-token") token: String,
        @Query("query") query: String,
        @Query("type") type: String,
        @Query("pageNumber") pageNumber: Int,
        @Query("documentLimit") documentLimit: Int,
    ): Call<FAQsModal>

    @POST("contact-us")
    @FormUrlEncoded
    fun contactUs(
//        @Header("x-access-token") token: String,
        @Field("name") name: String,
        @Field("email") email: String,
        @Field("message") message: String,
    ): Call<ContactUsModal>

    @GET("user/subscription")
    fun getSubscriptionsData(
        @Header("x-access-token") token: String
    ): Call<SubscriptionsModal>

    @DELETE("user/subscription")
    fun cancelSubscription(
        @Header("x-access-token") token: String,
    ): Call<CancelSubscriptions>

    @GET("posts"+"/{id}")
    fun getAllPosts(
        @Header("x-access-token") token: String,
        @Path(value = "id",encoded = true) id: String
    ): Call<ViewPostEventModal>

    @GET("posts"+"/{id}")
    fun getSinglePosts(
        @Header("x-access-token") token: String,
        @Path(value = "id",encoded = true) id: String
    ): Call<SharePostModal>

    @POST("business/insights")
    @FormUrlEncoded
    fun visitWebsite(
        @Header("x-access-token") token: String,
        @Field("type") type: String,
        @Field("businessProfileID") businessProfileID: String
    ): Call<WebsiteRedirectModal>

    @POST("user/blocks"+"/{id}")
    @FormUrlEncoded
    fun blockUser(
        @Header("x-access-token") token: String,
        @Path(value = "id",encoded = true) id: String,
        @Field("demo") demo: String
    ): Call<BlockUserModel>

    @GET("business/insights")
    fun insight(
        @Header("x-access-token") token: String,
        @Query("filter") filter: String,
    ): Call<InsightModal>

    @POST("business/insights")
    @FormUrlEncoded
    fun accountReach(
        @Header("x-access-token") token: String,
        @Field("type") type: String,
        @Field("businessProfileID") businessProfileID: String,
    ): Call<AccountReachModal>

    @POST("user/address")
    @FormUrlEncoded
    fun updateAddress(
        @Header("x-access-token") token: String,
        @Field("street") street: String,
        @Field("city") city: String,
        @Field("state") state: String,
        @Field("zipCode") zipCode: String,
        @Field("country") country: String,
        @Field("lat") lat: String,
        @Field("lng") lng: String
    ): Call<UpdateAddressModal>

    @DELETE("story" + "/{id}")
    fun deleteStory(
        @Header("x-access-token") token: String,
        @Path(value = "id", encoded = true) storyId: String
    ): Call<DeleteStoryModal>

    @POST("story/views" + "/{id}")
    @FormUrlEncoded
    fun viewStory(
        @Header("x-access-token") token: String,
        @Path(value = "id", encoded = true) storyId: String,
        @Field("demo") demo: String,
    ): Call<StoryActionModal>

    @POST("story/likes" + "/{id}")
    @FormUrlEncoded
    fun likeStory(
        @Header("x-access-token") token: String,
        @Path(value = "id", encoded = true) storyId: String,
        @Field("demo") demo: String,
    ): Call<StoryActionModal>

    @GET("story/likes" + "/{id}")
    fun getLikes(
        @Header("x-access-token") token: String,
        @Path(value = "id", encoded = true) storyId: String,
        @Query("pageNumber") pageNumber: Int,
        @Query("documentLimit") documentLimit: Int,
    ): Call<LikesModal>

    @GET("story/views" + "/{id}")
    fun getViewers(
        @Header("x-access-token") token: String,
        @Path(value = "id", encoded = true) storyId: String,
        @Query("pageNumber") pageNumber: Int,
        @Query("documentLimit") documentLimit: Int,
    ): Call<LikesModal>

    @retrofit2.http.Headers("Content-Type: application/json")
    @POST("posts/{postID}/publish-as-story")
    fun publishPostToStory(
        @Header("Authorization") authorization: String,
        @Path(value = "postID", encoded = true) postId: String,
        @retrofit2.http.Body body: Map<String, @JvmSuppressWildcards Any>
    ): Call<PublishStoryModal>

    @Multipart
    @POST("user/messaging/media-message")
    fun sendMediaMessage(
        @Header("x-access-token") token: RequestBody,
        @Part("username") username: RequestBody,
        @Part("message") message: RequestBody,
        @Part("messageType") messageType: RequestBody,
        @Part mediaType: MultipartBody.Part
    ): Call<SendMediaModal>

    @POST("user/report" + "/{id}")
    @FormUrlEncoded
    fun reportUser(
        @Header("x-access-token") token: String,
        @Path(value = "id", encoded = true) userId: String,
        @Field("reason") reason: String,
    ): Call<ReportUserModal>

    @POST("posts/reports" + "/{id}")
    @FormUrlEncoded
    fun reportPost(
        @Header("x-access-token") token: String,
        @Path(value = "id", encoded = true) postId: String,
        @Field("reason") reason: String,
    ): Call<ReportUserModal>

    @POST("posts/comments/reports" + "/{id}")
    @FormUrlEncoded
    fun reportComment(
        @Header("x-access-token") token: String,
        @Path(value = "id", encoded = true) commentId: String,
        @Field("reason") reason: String,
    ): Call<ReportUserModal>

    @GET("share/posts")
    fun getSharedPosts(
        @Header("x-access-token") token: String,
        @Query("postID") postID: String,
        @Query("userID") userID: String,
    ): Call<SharePostModal>

    @GET("user/business-profile/documents")
    fun getSavedDocuments(
        @Header("x-access-token") token: String
    ): Call<GetBusinessDocumentsModal>

    @GET("notifications/status")
    fun getNotificationStatus(
        @Header("x-access-token") token: String,
    ): Call<NotificationStatusModal>

    @POST("bookings/check-in")
    @FormUrlEncoded
    fun bookingCheckIn(
        @Header("x-access-token") token: String,
        @Field("businessProfileID") businessProfileID: String,
        @Field("checkIn") checkIn: String,
        @Field("checkOut") checkOut: String,
        @Field("adults") adultsCount: Int,
        @Field("children") childrenCount: Int,
        @Field("childrenAge") childrenAge: List<Int>,
        @Field("isTravellingWithPet") isTravellingWithPet: Boolean,
    ): Call<BookingCheckInModal>

    @GET("rooms" + "/{id}")
    fun fetchRoomDetails(
        @Header("x-access-token") token: String,
        @Path(value = "id", encoded = true) roomId: String,
    ): Call<RoomDetailsModal>

    @POST("bookings/checkout")
    @FormUrlEncoded
    fun bookingCheckOut(
        @Header("x-access-token") token: String,
        @Field("bookingID") bookingID: String,
        @Field("roomID") roomID: String,
        @Field("quantity") quantity: Int,
        @Field("bookedFor") bookedFor: String,
        @Field("promoCode") promoCode: String,
        @Field("price") price: Double,
    ): Call<BookingCheckOutModal>

    @POST("bookings/checkout/confirm")
    @FormUrlEncoded
    fun bookRoom(
        @Header("x-access-token") token: String,
        @Field("bookingID") bookingId: String,
        @Field("paymentID") paymentId: String,
        @Field("signature") signature: String,
        @Field("guestDetails") guestDetails: List<String>,
        @Field("bookedFor") bookedFor: String,
    ): Call<BookRoomModal>

    @GET("bookings")
    fun getBookingHistory(
        @Header("x-access-token") token: String,
        @Query("pageNumber") pageNumber: Int,
        @Query("documentLimit") documentLimit: Int,
    ): Call<BookingHistoryModal>

    @GET("bookings" + "/{id}")
    fun getBookingSummary(
        @Header("x-access-token") token: String,
        @Path(value = "id", encoded = true) bookingId: String,
    ): Call<BookingSummaryModal>


    @PATCH("bookings" + "/{id}" +"/change-status")
    @FormUrlEncoded
    fun acceptRejectTableBanquet(
        @Header("x-access-token") token: String,
        @Path(value = "id", encoded = true) bookingId: String,
        @Field("status") status: String
    ): Call<AcceptRejectBookingModal>



    @GET("bookings"+"/{bookingID}"+"/invoice")
    fun downloadBookingInvoice(
        @Header("x-access-token") token: String,
        @Path(value = "bookingID",encoded = true) bookingID: String,
    ): Call<ExportChatModal>

    @DELETE("bookings"+"/{bookingId}")
    fun cancelBooking(
        @Header("x-access-token") token: String,
        @Path(value = "bookingId",encoded = true) bookingId: String,
    ): Call<DeleteModal>

    @POST("auth/mobile/request-otp")
    @FormUrlEncoded
    fun sentOtpToNumber(
        @Header("x-access-token") token: String,
        @Field("dialCode") dialCode: String,
        @Field("phoneNumber") phoneNumber: String
    ): Call<DeleteModal>

    @POST("auth/mobile/verify-otp")
    @FormUrlEncoded
    fun verifyNumberWithOtp(
        @Header("x-access-token") token: String,
        @Field("dialCode") dialCode: String,
        @Field("phoneNumber") phoneNumber: String,
        @Field("otp") otp: String
    ): Call<VerifyMobileNumberModal>

    @POST("bookings/table")
    @FormUrlEncoded
    fun bookATable(
        @Header("x-access-token") token: String,
        @Field("numberOfGuests") numberOfGuests: String,
        @Field("date") date: String,
        @Field("time") time: String,
        @Field("businessProfileID") businessProfileID: String,
    ): Call<BookTableModal>


    @POST("bookings/banquet")
    @FormUrlEncoded
    fun bookABanquet(
        @Header("x-access-token") token: String,
        @Field("checkIn") checkIn: String,
        @Field("checkOut") checkOut: String,
        @Field("businessProfileID") businessProfileID: String,
        @Field("numberOfGuests") numberOfGuests: Int,
        @Field("typeOfEvent") typeOfEvent: String,
    ): Call<BookBanquetModal>

    @POST("jobs")
    @FormUrlEncoded
    fun postJob(
        @Header("x-access-token") token: String,
        @Field("title") title: String,
        @Field("designation") designation: String,
        @Field("description") description: String,
        @Field("jobType") jobType: String,
        @Field("salary") salary: String,
        @Field("joiningDate") joiningDate: String,
        @Field("numberOfVacancies") numberOfVacancies: String,
        @Field("experience") experience: String
    ): Call<PostJobModal>


    @GET("jobs"+"/{jobId}")
    fun getJobDetails(
        @Header("x-access-token") token: String,
        @Path(value = "jobId",encoded = true) jobId: String,
    ): Call<JobDetailsModal>

    // Collaboration APIs
    @POST("collaboration/invite")
    @FormUrlEncoded
    fun collaborationInvite(
        @Header("x-access-token") token: String,
        @Field("postID") postID: String,
        @Field("invitedUserID") invitedUserID: String,
    ): Call<CollaborationActionModal>

    @POST("collaboration/respond")
    @FormUrlEncoded
    fun collaborationRespond(
        @Header("x-access-token") token: String,
        @Field("postID") postID: String,
        @Field("action") action: String,
    ): Call<CollaborationActionModal>

    @GET("collaboration")
    fun getCollaborationPosts(
        @Header("x-access-token") token: String,
    ): Call<CollaborationPostsModal>

    @GET("collaboration"+"/{postID}"+"/collaborators")
    fun getPostCollaborators(
        @Header("x-access-token") token: String,
        @Path(value = "postID", encoded = true) postID: String,
    ): Call<CollaboratorsListModal>

}