package com.thehotelmedia.android.viewModal.individualViewModal

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.liveData
import com.thehotelmedia.android.SocketModals.sendMedia.SendMediaModal
import com.thehotelmedia.android.UIState.UIState
import com.thehotelmedia.android.apiService.Retrofit
import com.thehotelmedia.android.customClasses.Constants.N_A
import com.thehotelmedia.android.modals.DeleteModal
import com.thehotelmedia.android.modals.SharePostModal
import com.thehotelmedia.android.modals.Stories.Stories
import com.thehotelmedia.android.modals.accountReach.AccountReachModal
import com.thehotelmedia.android.modals.authentication.verifyMobileNumber.VerifyMobileNumberModal
import com.thehotelmedia.android.modals.blockUser.BlockUserModel
import com.thehotelmedia.android.modals.booking.acceptReject.AcceptRejectBookingModal
import com.thehotelmedia.android.modals.booking.bookBanquet.BookBanquetModal
import com.thehotelmedia.android.modals.booking.bookRoom.BookRoomModal
import com.thehotelmedia.android.modals.booking.bookTable.BookTableModal
import com.thehotelmedia.android.modals.booking.bookingHistory.BookingHistoryData
import com.thehotelmedia.android.modals.booking.bookingSummary.BookingSummaryModal
import com.thehotelmedia.android.modals.booking.checkIn.BookingCheckInModal
import com.thehotelmedia.android.modals.booking.checkout.BookingCheckOutModal
import com.thehotelmedia.android.modals.booking.roomDetails.RoomDetailsModal
import com.thehotelmedia.android.modals.booking.roomsList.RoomsListModal
import com.thehotelmedia.android.modals.chat.exportChat.ExportChatModal
import com.thehotelmedia.android.modals.forms.taggedPeople.TaggedData
import com.thehotelmedia.android.modals.checkIn.NearByPlacesModel
import com.thehotelmedia.android.modals.checkinData.checkInData.CheckInDataModel
import com.thehotelmedia.android.modals.editProfile.EditProfileModal
import com.thehotelmedia.android.modals.helpAndSupport.faqs.FAQsData
import com.thehotelmedia.android.modals.feeds.createComment.CreateCommentModal
import com.thehotelmedia.android.modals.feeds.feed.Data
import com.thehotelmedia.android.modals.feeds.savePost.SavePostModal
import com.thehotelmedia.android.modals.followAction.FollowActionModal
import com.thehotelmedia.android.modals.followUser.FollowUserModal
import com.thehotelmedia.android.modals.followUser.UnfollowModel
import com.thehotelmedia.android.modals.followerFollowing.FollowFollowingData
import com.thehotelmedia.android.modals.forms.createEvent.CreateEventModal
import com.thehotelmedia.android.modals.forms.createPost.CreatePostModal
import com.thehotelmedia.android.modals.forms.createReviews.CreateReviewModel
import com.thehotelmedia.android.modals.forms.createStory.CreateStoryModal
import com.thehotelmedia.android.modals.getBusinessDoc.GetBusinessDocumentsModal
import com.thehotelmedia.android.modals.helpAndSupport.contactUs.ContactUsModal
import com.thehotelmedia.android.modals.insight.InsightModal
import com.thehotelmedia.android.modals.job.jobDetails.JobDetailsModal
import com.thehotelmedia.android.modals.notificationStatus.NotificationStatusModal
import com.thehotelmedia.android.modals.notifications.NotificationData
import com.thehotelmedia.android.modals.job.postJob.PostJobModal
import com.thehotelmedia.android.modals.profileData.image.ImageData
import com.thehotelmedia.android.modals.profileData.profile.GetProfileModal
import com.thehotelmedia.android.modals.report.ReportUserModal
import com.thehotelmedia.android.modals.search.SearchData
import com.thehotelmedia.android.modals.share.shareProfile.ShareProfileModal
import com.thehotelmedia.android.modals.storiesActions.DeleteStoryModal
import com.thehotelmedia.android.modals.storiesActions.likeStory.LikeData
import com.thehotelmedia.android.modals.storiesActions.likeViewStory.StoryActionModal
import com.thehotelmedia.android.modals.storiesActions.publishStory.PublishStoryModal
import com.thehotelmedia.android.modals.subscriptionDetails.SubscriptionData
import com.thehotelmedia.android.modals.subscriptions.CancelSubscriptions
import com.thehotelmedia.android.modals.subscriptions.SubscriptionsModal
import com.thehotelmedia.android.modals.suggestedBusiness.SuggestionData
import com.thehotelmedia.android.modals.transactions.TransactionData
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
import com.thehotelmedia.android.pagination.blockUsers.BlockedUserPagingSource
import com.thehotelmedia.android.pagination.bookings.BookingHistoryPagingSource
import com.thehotelmedia.android.pagination.faq.FAQPagingSource
import com.thehotelmedia.android.pagination.feed.CommentsPagingSource
import com.thehotelmedia.android.pagination.feed.FeedPagingSource
import com.thehotelmedia.android.pagination.feed.TaggedPeoplePagingSource
import com.thehotelmedia.android.pagination.followerFollowing.FollowerPagingSource
import com.thehotelmedia.android.pagination.followerFollowing.FollowingPagingSource
import com.thehotelmedia.android.pagination.notification.NotificationPagingSource
import com.thehotelmedia.android.pagination.profile.ImagePagingSource
import com.thehotelmedia.android.pagination.profile.PostsPagingSource
import com.thehotelmedia.android.pagination.profile.ReviewPagingSource
import com.thehotelmedia.android.pagination.profile.VideoPagingSource
import com.thehotelmedia.android.pagination.search.SearchPagingSource
import com.thehotelmedia.android.pagination.settings.SavedPostPagingSource
import com.thehotelmedia.android.pagination.stories.LikesPagingSource
import com.thehotelmedia.android.pagination.stories.StoriesPagingSource
import com.thehotelmedia.android.pagination.stories.ViewersPagingSource
import com.thehotelmedia.android.pagination.suggestion.SuggestionPagingSource
import com.thehotelmedia.android.pagination.transactions.TransactionPagingSource
import com.thehotelmedia.android.repository.IndividualRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import retrofit2.Response
import java.io.File
import org.json.JSONObject




class IndividualViewModal(private val individualRepo: IndividualRepo) : ViewModel(){

    private val tag = "INDIVIDUAL_VIEW_MODEL"
    private val toastMessageLiveData = MutableLiveData<String>()
    val toast: LiveData<String> = toastMessageLiveData
    private val reportToastMessageLiveData = MutableLiveData<String>()
    val reportToast: LiveData<String> = reportToastMessageLiveData

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _otpLoading = MutableLiveData<Boolean>()
    val otpLoading: LiveData<Boolean> = _otpLoading


    private val otpMessageLiveData = MutableLiveData<String>()
    val otpToast: LiveData<String> = otpMessageLiveData



    //get Weather
    private val _getWeatherResult = MutableLiveData<WeatherModal>()
    val getWeatherResult: LiveData<WeatherModal> = _getWeatherResult
    fun getWeather(lat: Double, lon: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = Retrofit.weatherApiService.getWeather(lat, lon, "14fda30b266c2f4f5aa64d08344926a6").execute()

                if (response.isSuccessful) {
                    response.body()?.let {
                        _getWeatherResult.postValue(it)
                    } ?: run {
//                        toastMessageLiveData.postValue("Error: Empty response body")
                    }
                } else {
//                    toastMessageLiveData.postValue("Error: ${response.message()}")
                }
            } catch (t: Throwable) {
//                toastMessageLiveData.postValue("Error: ${t.message}")
                Log.w(tag, "Error: ${t.message}", t)
            }
        }
    }
    //get AQI
    private val _getAQIResult = MutableLiveData<AqiModal>()
    val getAQIResult: LiveData<AqiModal> = _getAQIResult
    fun getAQI(lat: Double, lon: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = Retrofit.weatherApiService.getAQI(
                    lat,
                    lon,
                    "14fda30b266c2f4f5aa64d08344926a6"
                ).execute()

                if (response.isSuccessful) {
                    response.body()?.let {
                        _getAQIResult.postValue(it)
                    } ?: run {
//                        toastMessageLiveData.postValue("Error: Empty response body")
                    }
                } else {
//                    toastMessageLiveData.postValue("Error: ${response.message()}")
                }
            } catch (t: Throwable) {
//                toastMessageLiveData.postValue("Error: ${t.message}")
                Log.w(tag, "Error: ${t.message}", t)
            }
        }
    }



    //Export Chat
    private val _getSubscriptionDetailsResult = MutableLiveData<SubscriptionData>()
    val getSubscriptionDetailsResult: LiveData<SubscriptionData> = _getSubscriptionDetailsResult
    fun getSubscriptionDetails() {
        viewModelScope.launch(Dispatchers.IO) {
//            _loading.postValue(true)
            try {
                val response = individualRepo.getSubscriptionDetails()
                if (response.isSuccessful) {
//                    val res = response.body()
//                    toastMessageLiveData.postValue(res?.message)
                    _getSubscriptionDetailsResult.postValue(response.body())
                    Log.wtf(tag, response.body().toString())
//                    _loading.postValue(false)
                } else {
                    Log.wtf(tag + "ELSE", response.message().toString())
//                    toastMessageLiveData.postValue(response.message())
//                    _loading.postValue(false)
                }

            } catch (t: Throwable) {
//                _loading.postValue(false)
//                toastMessageLiveData.postValue(t.message)
                Log.wtf(tag + "ERROR", t.message.toString())
            }
        }
    }

    // Collaboration: Invite
    private val _collaborationInviteResult = MutableLiveData<CollaborationActionModal>()
    val collaborationInviteResult: LiveData<CollaborationActionModal> = _collaborationInviteResult
    fun collaborationInvite(postID: String, invitedUserID: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _loading.postValue(true)
            try {
                val response = individualRepo.collaborationInvite(postID, invitedUserID)
                if (response.isSuccessful) {
                    val res = response.body()
                    toastMessageLiveData.postValue(res?.message ?: N_A)
                    _collaborationInviteResult.postValue(res)
                    _loading.postValue(false)
                } else {
                    toastMessageLiveData.postValue(response.message())
                    _loading.postValue(false)
                }
            } catch (t: Throwable) {
                _loading.postValue(false)
                toastMessageLiveData.postValue(t.message)
            }
        }
    }

    // Collaboration: Respond
    private val _collaborationRespondResult = MutableLiveData<CollaborationActionModal>()
    val collaborationRespondResult: LiveData<CollaborationActionModal> = _collaborationRespondResult
    fun collaborationRespond(postID: String, action: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _loading.postValue(true)
            try {
                val response = individualRepo.collaborationRespond(postID, action)
                if (response.isSuccessful) {
                    val res = response.body()
                    toastMessageLiveData.postValue(res?.message ?: N_A)
                    _collaborationRespondResult.postValue(res)
                    _loading.postValue(false)
                } else {
                    toastMessageLiveData.postValue(response.message())
                    _loading.postValue(false)
                }
            } catch (t: Throwable) {
                _loading.postValue(false)
                toastMessageLiveData.postValue(t.message)
            }
        }
    }

    // Collaboration: List posts
    private val _collaborationPostsResult = MutableLiveData<CollaborationPostsModal>()
    val collaborationPostsResult: LiveData<CollaborationPostsModal> = _collaborationPostsResult
    fun getCollaborationPosts() {
        viewModelScope.launch(Dispatchers.IO) {
            _loading.postValue(true)
            try {
                val response = individualRepo.getCollaborationPosts()
                if (response.isSuccessful) {
                    _collaborationPostsResult.postValue(response.body())
                    _loading.postValue(false)
                } else {
                    toastMessageLiveData.postValue(response.message())
                    _loading.postValue(false)
                }
            } catch (t: Throwable) {
                _loading.postValue(false)
                toastMessageLiveData.postValue(t.message)
            }
        }
    }

    // Collaboration: List collaborators per post
    private val _postCollaboratorsResult = MutableLiveData<CollaboratorsListModal>()
    val postCollaboratorsResult: LiveData<CollaboratorsListModal> = _postCollaboratorsResult
    fun getPostCollaborators(postID: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _loading.postValue(true)
            try {
                val response = individualRepo.getPostCollaborators(postID)
                if (response.isSuccessful) {
                    _postCollaboratorsResult.postValue(response.body())
                    _loading.postValue(false)
                } else {
                    toastMessageLiveData.postValue(response.message())
                    _loading.postValue(false)
                }
            } catch (t: Throwable) {
                _loading.postValue(false)
                toastMessageLiveData.postValue(t.message)
            }
        }
    }
    //Export Chat
    private val _deleteChatResult = MutableLiveData<DeleteModal>()
    val deleteChatResult: LiveData<DeleteModal> = _deleteChatResult
    fun deleteChat(userID: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _loading.postValue(true)
            try {
                val response = individualRepo.deleteChat(userID)
                if (response.isSuccessful) {
                    val res = response.body()
                    toastMessageLiveData.postValue(res?.message ?: N_A)
                    _deleteChatResult.postValue(response.body())
                    Log.wtf(tag, response.body().toString())
                    _loading.postValue(false)
                } else {
                    Log.wtf(tag + "ELSE", response.message().toString())
                    toastMessageLiveData.postValue(response.message())
                    _loading.postValue(false)
                }

            } catch (t: Throwable) {
                _loading.postValue(false)
                toastMessageLiveData.postValue(t.message)
                Log.wtf(tag + "ERROR", t.message.toString())
            }
        }
    }
    //Delete Post
    private val _deletePostResult = MutableLiveData<DeleteModal>()
    val deletePostResult: LiveData<DeleteModal> = _deletePostResult
    fun deletePost(postID: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _loading.postValue(true)
            try {
                val response = individualRepo.deletePost(postID)
                if (response.isSuccessful) {
                    val res = response.body()
                    toastMessageLiveData.postValue(res?.message ?: N_A)
                    _deletePostResult.postValue(response.body())
                    Log.wtf(tag, response.body().toString())
                    _loading.postValue(false)
                } else {
                    Log.wtf(tag + "ELSE", response.message().toString())
                    toastMessageLiveData.postValue(response.message())
                    _loading.postValue(false)
                }

            } catch (t: Throwable) {
                _loading.postValue(false)
                toastMessageLiveData.postValue(t.message)
                Log.wtf(tag + "ERROR", t.message.toString())
            }
        }
    }

    // Update Post
    private val _updatePostResult = MutableLiveData<DeleteModal>()
    val updatePostResult: LiveData<DeleteModal> = _updatePostResult
    fun updatePost(postID: String, content: String, feelings: String, media: List<String>, deletedMedia: List<String>, placeName: String = "", lat: Double = 0.0, lng: Double = 0.0) {
        viewModelScope.launch(Dispatchers.IO) {
            _loading.postValue(true)
            try {
                val response = individualRepo.updatePost(postID, content, feelings, media, deletedMedia, placeName, lat, lng)
                if (response.isSuccessful) {
                    try {
                        val res = response.body()
                        toastMessageLiveData.postValue(res?.message ?: N_A)
                        _updatePostResult.postValue(res)
                        _loading.postValue(false)
                    } catch (e: Exception) {
                        // Handle JSON parsing error
                        Log.e(tag, "Error parsing response: ${e.message}", e)
                        val errorModal = DeleteModal().apply {
                            status = false
                            message = "Error parsing response: ${e.message}"
                        }
                        toastMessageLiveData.postValue(errorModal.message)
                        _updatePostResult.postValue(errorModal)
                        _loading.postValue(false)
                    }
                } else {
                    val errorMessage = try {
                        response.errorBody()?.string() ?: response.message() ?: "Failed to update post"
                    } catch (e: Exception) {
                        response.message() ?: "Failed to update post"
                    }
                    val errorModal = DeleteModal().apply {
                        status = false
                        message = errorMessage
                    }
                    toastMessageLiveData.postValue(errorModal.message)
                    _updatePostResult.postValue(errorModal)
                    _loading.postValue(false)
                }
            } catch (t: Throwable) {
                Log.e(tag, "Error updating post: ${t.message}", t)
                val errorModal = DeleteModal().apply {
                    status = false
                    message = t.message ?: "An error occurred"
                }
                _updatePostResult.postValue(errorModal)
                _loading.postValue(false)
                toastMessageLiveData.postValue(t.message)
            }
        }
    }
    //Export Chat
    private val _exportChatResult = MutableLiveData<ExportChatModal>()
    val exportChatResult: LiveData<ExportChatModal> = _exportChatResult
    fun exportChat(userID: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _loading.postValue(true)
            try {
                val response = individualRepo.exportChat(userID)
                if (response.isSuccessful) {
//                    val res = response.body()
//                    toastMessageLiveData.postValue(res?.message)
                    _exportChatResult.postValue(response.body())
                    Log.wtf(tag, response.body().toString())
                    _loading.postValue(false)
                } else {
                    Log.wtf(tag + "ELSE", response.message().toString())
                    toastMessageLiveData.postValue(response.message())
                    _loading.postValue(false)
                }

            } catch (t: Throwable) {
                _loading.postValue(false)
                toastMessageLiveData.postValue(t.message)
                Log.wtf(tag + "ERROR", t.message.toString())
            }
        }
    }

    //View Media
    private val _viewMediaResult = MutableLiveData<ViewMediaModal>()
    val viewMediaResult: LiveData<ViewMediaModal> = _viewMediaResult
    fun viewMedia(postId: String,mediaId: String) {
        viewModelScope.launch(Dispatchers.IO) {
//            _loading.postValue(true)
            try {
                val response = individualRepo.viewMedia(postId,mediaId)
                if (response.isSuccessful) {
//                    val res = response.body()
//                    toastMessageLiveData.postValue(res?.message)
                    _viewMediaResult.postValue(response.body())
                    Log.wtf(tag, response.body().toString())
//                    _loading.postValue(false)
                } else {
                    Log.wtf(tag + "ELSE", response.message().toString())
                    toastMessageLiveData.postValue(response.message())
//                    _loading.postValue(false)
                }

            } catch (t: Throwable) {
//                _loading.postValue(false)
                toastMessageLiveData.postValue(t.message)
                Log.wtf(tag + "ERROR", t.message.toString())
            }
        }
    }

    //Post Views
    private val _postViewsResult = MutableLiveData<DeleteModal>()
    val postViewsResult: LiveData<DeleteModal> = _postViewsResult
    fun postViews(postIds: List<String>) {
        println("asfhajksdhfjk  $postIds")

        viewModelScope.launch(Dispatchers.IO) {
//            _loading.postValue(true)
            try {
                val response = individualRepo.postViews(postIds)
                if (response.isSuccessful) {
//                    val res = response.body()
//                    toastMessageLiveData.postValue(res?.message)
                    _postViewsResult.postValue(response.body())
                    Log.wtf(tag, response.body().toString())
//                    _loading.postValue(false)
                } else {
                    Log.wtf(tag + "ELSE", response.message().toString())
//                    toastMessageLiveData.postValue(response.message())
//                    _loading.postValue(false)
                }

            } catch (t: Throwable) {
//                _loading.postValue(false)
//                toastMessageLiveData.postValue(t.message)
                Log.wtf(tag + "ERROR", t.message.toString())
            }
        }
    }

    //Share Profile
    private val _shareProfileResult = MutableLiveData<ShareProfileModal>()
    val shareProfileResult: LiveData<ShareProfileModal> = _shareProfileResult
    fun shareProfile(id: String,userID: String) {
        viewModelScope.launch(Dispatchers.IO) {
//            _loading.postValue(true)
            try {
                val response = individualRepo.shareProfileData(id,userID)
                if (response.isSuccessful) {
//                    val res = response.body()
//                    toastMessageLiveData.postValue(res?.message)
                    _shareProfileResult.postValue(response.body())
                    Log.wtf(tag, response.body().toString())
//                    _loading.postValue(false)
                } else {
                    Log.wtf(tag + "ELSE", response.message().toString())
                    toastMessageLiveData.postValue(response.message())
//                    _loading.postValue(false)
                }

            } catch (t: Throwable) {
//                _loading.postValue(false)
                toastMessageLiveData.postValue(t.message)
                Log.wtf(tag + "ERROR", t.message.toString())
            }
        }
    }

    //Get Profile
    private val _getProfileResult = MutableLiveData<GetProfileModal>()
    val getProfileResult: LiveData<GetProfileModal> = _getProfileResult
    fun getProfile() {
        viewModelScope.launch(Dispatchers.IO) {
            _loading.postValue(true)
            try {
                val response = individualRepo.getProfile()
                if (response.isSuccessful) {
//                    val res = response.body()
//                    toastMessageLiveData.postValue(res?.message)
                    _getProfileResult.postValue(response.body())
                    Log.wtf(tag, response.body().toString())
                    _loading.postValue(false)
                } else {
                    Log.wtf(tag + "ELSE", response.message().toString())
                    toastMessageLiveData.postValue(response.message())
                    _loading.postValue(false)
                }

            } catch (t: Throwable) {
                _loading.postValue(false)
                toastMessageLiveData.postValue(t.message)
                Log.wtf(tag + "ERROR", t.message.toString())
            }
        }
    }

    //Update Language
    private val _updateLanguageResult = MutableLiveData<EditProfileModal>()
    val updateLanguageResult: LiveData<EditProfileModal> = _updateLanguageResult
    fun updateLanguage(language: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _loading.postValue(true)
            try {
                val response = individualRepo.updateLanguage(language)
                if (response.isSuccessful) {
//                    val res = response.body()
//                    toastMessageLiveData.postValue(res?.message ?: N_A)
                    _updateLanguageResult.postValue(response.body())
                    Log.wtf(tag, response.body().toString())
                    _loading.postValue(false)
                } else {
                    Log.wtf(tag + "ELSE", response.message().toString())
                    toastMessageLiveData.postValue(response.message())
                    _loading.postValue(false)
                }

            } catch (t: Throwable) {
                _loading.postValue(false)
                toastMessageLiveData.postValue(t.message)
                Log.wtf(tag + "ERROR", t.message.toString())
            }
        }
    }

    //Edit Profile
    private val _editProfileResult = MutableLiveData<EditProfileModal>()
    val editProfileResult: LiveData<EditProfileModal> = _editProfileResult
    fun editProfile(name: String,email: String,dialCode: String,phoneNumber: String,bio: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _loading.postValue(true)
            try {
                val response = individualRepo.editProfile(name,email,dialCode, phoneNumber, bio)
                if (response.isSuccessful) {
                    val res = response.body()
                    toastMessageLiveData.postValue(res?.message ?: N_A)
                    _editProfileResult.postValue(response.body())
                    Log.wtf(tag, response.body().toString())
                    _loading.postValue(false)
                } else {
                    Log.wtf(tag + "ELSE", response.message().toString())
                    toastMessageLiveData.postValue(response.message())
                    _loading.postValue(false)
                }

            } catch (t: Throwable) {
                _loading.postValue(false)
                toastMessageLiveData.postValue(t.message)
                Log.wtf(tag + "ERROR", t.message.toString())
            }
        }
    }

    //Update Profile Status
    private val _updateStatusResult = MutableLiveData<EditProfileModal>()
    val updateStatusResult: LiveData<EditProfileModal> = _updateStatusResult
    fun updateStatus(privateAccount: Boolean,notificationEnabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            _loading.postValue(true)
            try {
                val response = individualRepo.updateStatus(privateAccount,notificationEnabled)
                if (response.isSuccessful) {
//                    val res = response.body()
//                    toastMessageLiveData.postValue(res?.message)
                    _updateStatusResult.postValue(response.body())
                    Log.wtf(tag, response.body().toString())
                    _loading.postValue(false)
                } else {
                    Log.wtf(tag + "ELSE", response.message().toString())
                    toastMessageLiveData.postValue(response.message())
                    _loading.postValue(false)
                }

            } catch (t: Throwable) {
                _loading.postValue(false)
                toastMessageLiveData.postValue(t.message)
                Log.wtf(tag + "ERROR", t.message.toString())
            }
        }
    }

    //Edit Category
    private val _editCategoryResult = MutableLiveData<EditProfileModal>()
    val editCategoryResult: LiveData<EditProfileModal> = _editCategoryResult
    fun editCategory(businessTypeID: String,businessSubTypeID: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _loading.postValue(true)
            try {
                val response = individualRepo.editCategory(businessTypeID,businessSubTypeID)
                if (response.isSuccessful) {
//                    val res = response.body()
//                    toastMessageLiveData.postValue(res?.message)
                    _editCategoryResult.postValue(response.body())
                    Log.wtf(tag, response.body().toString())
                    _loading.postValue(false)
                } else {
                    Log.wtf(tag + "ELSE", response.message().toString())
                    toastMessageLiveData.postValue(response.message())
                    _loading.postValue(false)
                }

            } catch (t: Throwable) {
                _loading.postValue(false)
                toastMessageLiveData.postValue(t.message)
                Log.wtf(tag + "ERROR", t.message.toString())
            }
        }
    }



    //NearBy Places
    private val _getNearbyPlacesResult = MutableLiveData<NearByPlacesModel>()
    val getNearbyPlacesResult: LiveData<NearByPlacesModel> = _getNearbyPlacesResult

    fun getNearbyPlaces(lat: String,lng: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _loading.postValue(true) // Indicate loading started
            try {
                val response: Response<NearByPlacesModel> = individualRepo.getNearbyPlaces(lat,lng)
                if (response.isSuccessful) {
                    _getNearbyPlacesResult.postValue(response.body())
                    Log.wtf(tag, response.body().toString())
                } else {
                    Log.wtf(tag + "ELSE", response.message().toString())
                    toastMessageLiveData.postValue(response.message())
                }
            } catch (t: Throwable) {
                Log.wtf(tag + "ERROR", t.message.toString())
                toastMessageLiveData.postValue(t.message)
            } finally {
                _loading.postValue(false) // Indicate loading finished
            }
        }
    }



    //Get CheckInData
    private val _getCheckInDataResult = MutableLiveData<CheckInDataModel>()
    val getCheckInDataResult: LiveData<CheckInDataModel> = _getCheckInDataResult
    fun getCheckInData(placeId: String,businessProfileID: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _loading.postValue(true)
            try {
                val response = individualRepo.getCheckInData(placeId,businessProfileID)
                if (response.isSuccessful) {
//                    val res = response.body()
//                    toastMessageLiveData.postValue(res?.message)
                    _getCheckInDataResult.postValue(response.body())
                    Log.wtf(tag, response.body().toString())
                    _loading.postValue(false)
                } else {
                    Log.wtf(tag + "ELSE", response.message().toString())
//                    toastMessageLiveData.postValue(response.message())
                    _loading.postValue(false)
                }

            } catch (t: Throwable) {
                _loading.postValue(false)
                toastMessageLiveData.postValue(t.message)
                Log.wtf(tag + "ERROR", t.message.toString())
            }
        }
    }


    //CreateNewPost
    private val _createNewPostResult = MutableLiveData<CreatePostModal>()
    val createNewPostResult: LiveData<CreatePostModal> = _createNewPostResult
    fun createNewPost(content: String, placeName: String, lat: Double, lng: Double, feelings: String, mediaList: List<String>, taggedList: List<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            _loading.postValue(true)
            try {
                val response = individualRepo.createNewPost(content,placeName,lat,lng,feelings,mediaList,taggedList)
                if (response.isSuccessful) {
//                    val res = response.body()
//                    toastMessageLiveData.postValue(res?.message)
                    _createNewPostResult.postValue(response.body())
                    Log.wtf(tag, response.body().toString())
                    _loading.postValue(false)
                } else {
                    Log.wtf(tag + "ELSE", response.toString())
                    toastMessageLiveData.postValue(response.message())
                    _loading.postValue(false)
                }

            } catch (t: Throwable) {
                _loading.postValue(false)
                toastMessageLiveData.postValue(t.message)
                Log.wtf(tag + "ERROR", t.message.toString())
            }
        }
    }



    //Get CheckInData
    private val _createReviewResult = MutableLiveData<CreateReviewModel>()
    val createReviewResult: LiveData<CreateReviewModel> = _createReviewResult
    fun createReview(
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
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _loading.postValue(true)
            try {
                val response = individualRepo.createReview(businessProfileId,content,placeId,reviews,name,street, city,state,zipCode,country,lat,lng,imageList,videoList,anonymousUserID)
                if (response.isSuccessful) {
//                    val res = response.body()
//                    toastMessageLiveData.postValue(res?.message)
                    _createReviewResult.postValue(response.body())
                    Log.wtf(tag, response.body().toString())
                    _loading.postValue(false)
                } else {
                    Log.wtf(tag + "ELSE", response.message().toString())
                    toastMessageLiveData.postValue(response.message())
                    _loading.postValue(false)
                }

            } catch (t: Throwable) {
                _loading.postValue(false)
                toastMessageLiveData.postValue(t.message)
                Log.wtf(tag + "ERROR", t.message.toString())
            }
        }
    }



    fun getFeeds(lat: Double,lng: Double): LiveData<PagingData<Data>> {

        return Pager(
            config = PagingConfig(
                pageSize = 15, // Increase page size for fewer network requests
                prefetchDistance = 5, // Start loading the next page when 1 items remain
                enablePlaceholders = false // Disable placeholders for smoother scrolling
            ),
            pagingSourceFactory = { FeedPagingSource(individualRepo,lat,lng) }
        ).liveData.cachedIn(viewModelScope)
    }


    fun getStories(): LiveData<PagingData<Stories>> {
        return Pager(
            config = PagingConfig(pageSize = 20),
            pagingSourceFactory = { StoriesPagingSource(individualRepo) }
        ).liveData.cachedIn(viewModelScope)
    }

    fun getSavedPost(): LiveData<PagingData<Data>> {
        return Pager(
            config = PagingConfig(pageSize = 20),
            pagingSourceFactory = { SavedPostPagingSource( individualRepo) }
        ).liveData.cachedIn(viewModelScope)

    }


    //Create Event
    private val _createEventResult = MutableLiveData<CreateEventModal>()
    val createEventResult: LiveData<CreateEventModal> = _createEventResult
    fun createEvent(name: String, startDate: String, startTime: String, endDate: String, endTime: String, type: String, venueName: String, streamingLink: String, description: String,placeName: String,lat: String,lng: String, imageFile: File) {
        viewModelScope.launch(Dispatchers.IO) {
            _loading.postValue(true)
            try {
                val response = individualRepo.createEvent(name, startDate, startTime,endDate,endTime, type, venueName, streamingLink, description,placeName, lat, lng, imageFile)
                if (response.isSuccessful) {
//                    val res = response.body()
//                    toastMessageLiveData.postValue(res?.message)
                    _createEventResult.postValue(response.body())
                    Log.wtf(tag, response.body().toString())
                    _loading.postValue(false)
                } else {
                    Log.wtf(tag + "ELSE", response.message().toString())
                    toastMessageLiveData.postValue(response.message())
                    _loading.postValue(false)
                }

            } catch (t: Throwable) {
                _loading.postValue(false)
                toastMessageLiveData.postValue(t.message)
                Log.wtf(tag + "ERROR", t.message.toString())
            }
        }
    }

    //Create Story
    private val _createStoryResult = MutableLiveData<CreateStoryModal>()
    val createStoryResult: LiveData<CreateStoryModal> = _createStoryResult
    fun createStory(imageFile: File?, videoFile: File?, taggedIds: List<String>, placeName: String?, lat: Double?, lng: Double?, locationX: Float? = null, locationY: Float? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            _loading.postValue(true)
            try {
                val response = individualRepo.createStory(imageFile, videoFile, taggedIds, placeName, lat, lng, locationX, locationY)
                if (response.isSuccessful) {
//                    val res = response.body()
//                    toastMessageLiveData.postValue(res?.message)
                    _createStoryResult.postValue(response.body())
                    Log.wtf(tag, response.body().toString())
                    _loading.postValue(false)
                } else {
                    Log.wtf(tag + "ELSE", response.message().toString())
                    toastMessageLiveData.postValue(response.message())
                    _loading.postValue(false)
                }

            } catch (t: Throwable) {
                _loading.postValue(false)
                toastMessageLiveData.postValue(t.message)
                Log.wtf(tag + "ERROR", t.message.toString())
            }
        }
    }

    private val _publishStoryResult = MutableLiveData<PublishStoryModal>()
    val publishStoryResult: LiveData<PublishStoryModal> = _publishStoryResult
    fun publishPostToStory(postId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = individualRepo.publishPostToStory(postId)
                if (response.isSuccessful) {
                    val res = response.body()
                    toastMessageLiveData.postValue(res?.message ?: N_A)
                    _publishStoryResult.postValue(res)
                    Log.wtf(tag, res.toString())
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.wtf(tag + "ELSE", "${response.code()} $errorBody")
                    val message = try {
                        JSONObject(errorBody ?: "{}").optString("message")
                    } catch (e: Exception) {
                        null
                    }
                    toastMessageLiveData.postValue(message?.takeIf { it.isNotBlank() } ?: response.message())
                    _publishStoryResult.postValue(null)
                }
            } catch (t: Throwable) {
                toastMessageLiveData.postValue(t.message)
                _publishStoryResult.postValue(null)
                Log.wtf(tag + "ERROR", t.message.toString())
            }
        }
    }

    //SavePost
    private val _savePostResult = MutableLiveData<SavePostModal>()
    val savePostResult: LiveData<SavePostModal> = _savePostResult
    fun savePost(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
//            _loading.postValue(true)
            try {
                val response = individualRepo.savePost(id)
                if (response.isSuccessful) {
//                    val res = response.body()
//                    toastMessageLiveData.postValue(res?.message)
                    _savePostResult.postValue(response.body())
                    Log.wtf(tag, response.body().toString())
                    _loading.postValue(false)
                } else {
                    Log.wtf(tag + "ELSE", response.message().toString())
                    toastMessageLiveData.postValue(response.message())
                    _loading.postValue(false)
                }

            } catch (t: Throwable) {
                _loading.postValue(false)
                toastMessageLiveData.postValue(t.message)
                Log.wtf(tag + "ERROR", t.message.toString())
            }
        }
    }

    //LikePost
    private val _likePostResult = MutableLiveData<SavePostModal>()
    val likePostResult: LiveData<SavePostModal> = _likePostResult
    fun likePost(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
//            _loading.postValue(true)
            try {
                val response = individualRepo.likePost(id)
                if (response.isSuccessful) {
//                    val res = response.body()
//                    toastMessageLiveData.postValue(res?.message)
                    _likePostResult.postValue(response.body())
                    Log.wtf(tag, response.body().toString())
                    _loading.postValue(false)
                } else {
                    Log.wtf(tag + "ELSE", response.message().toString())
                    toastMessageLiveData.postValue(response.message())
                    _loading.postValue(false)
                }

            } catch (t: Throwable) {
                _loading.postValue(false)
                toastMessageLiveData.postValue(t.message)
                Log.wtf(tag + "ERROR", t.message.toString())
            }
        }
    }

    //Join Event
    private val _joinEventResult = MutableLiveData<SavePostModal>()
    val joinEventResult: LiveData<SavePostModal> = _joinEventResult
    fun joinEvent(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
//            _loading.postValue(true)
            try {
                val response = individualRepo.joinEvent(id)
                if (response.isSuccessful) {
//                    val res = response.body()
//                    toastMessageLiveData.postValue(res?.message)
                    _joinEventResult.postValue(response.body())
                    Log.wtf(tag, response.body().toString())
//                    _loading.postValue(false)
                } else {
                    Log.wtf(tag + "ELSE", response.message().toString())
                    toastMessageLiveData.postValue(response.message())
                    _loading.postValue(false)
                }

            } catch (t: Throwable) {
//                _loading.postValue(false)
                toastMessageLiveData.postValue(t.message)
                Log.wtf(tag + "ERROR", t.message.toString())
            }
        }
    }


    //PostedWorkRequirement Data
    fun getComments(id: String): LiveData<PagingData<com.thehotelmedia.android.modals.feeds.getComments.Data>> {
        return Pager(
            config = PagingConfig(pageSize = 20),
            pagingSourceFactory = { CommentsPagingSource( id,individualRepo) }
        ).liveData.cachedIn(viewModelScope)

    }



    //LikePost
    private val _createCommentResult = MutableLiveData<CreateCommentModal>()
    val createCommentResult: LiveData<CreateCommentModal> = _createCommentResult
    fun createComment(postID: String,message: String,parentID: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _loading.postValue(true)
            try {
                val response = individualRepo.createComment(postID,message,parentID)
                if (response.isSuccessful) {
//                    val res = response.body()
//                    toastMessageLiveData.postValue(res?.message)
                    _createCommentResult.postValue(response.body())
                    Log.wtf(tag, response.body().toString())
                    _loading.postValue(false)
                } else {
                    Log.wtf(tag + "ELSE", response.message().toString())
                    toastMessageLiveData.postValue(response.message())
                    _loading.postValue(false)
                }

            } catch (t: Throwable) {
                _loading.postValue(false)
                toastMessageLiveData.postValue(t.message)
                Log.wtf(tag + "ERROR", t.message.toString())
            }
        }
    }

    //LikeComments
    private val _likeCommentsResult = MutableLiveData<SavePostModal>()
    val likeCommentsResult: LiveData<SavePostModal> = _likeCommentsResult
    fun likeComments(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _loading.postValue(true)
            try {
                val response = individualRepo.likeComments(id)
                if (response.isSuccessful) {
//                    val res = response.body()
//                    toastMessageLiveData.postValue(res?.message)
                    _likeCommentsResult.postValue(response.body())
                    Log.wtf(tag, response.body().toString())
                    _loading.postValue(false)
                } else {
                    Log.wtf(tag + "ELSE", response.message().toString())
                    toastMessageLiveData.postValue(response.message())
                    _loading.postValue(false)
                }

            } catch (t: Throwable) {
                _loading.postValue(false)
                toastMessageLiveData.postValue(t.message)
                Log.wtf(tag + "ERROR", t.message.toString())
            }
        }
    }

    fun getVideos(id: String): LiveData<PagingData<com.thehotelmedia.android.modals.profileData.video.Data>> {
        return Pager(
            config = PagingConfig(pageSize = 8),
            pagingSourceFactory = { VideoPagingSource( id,individualRepo) }
        ).liveData.cachedIn(viewModelScope)

    }

    fun getImages(id: String): LiveData<PagingData<ImageData>> {
        return Pager(
            config = PagingConfig(pageSize = 8),
            pagingSourceFactory = { ImagePagingSource( id,individualRepo) }
        ).liveData.cachedIn(viewModelScope)

    }

    fun getTagged(search: String): LiveData<PagingData<TaggedData>> {
        return Pager(
            config = PagingConfig(pageSize = 20),
            pagingSourceFactory = { TaggedPeoplePagingSource(search, individualRepo) }
        ).liveData.cachedIn(viewModelScope)
    }

    // Get all users for collaboration (searches all users in database, not just followers)
    fun getCollaborationUsers(search: String): LiveData<PagingData<TaggedData>> {
        return Pager(
            config = PagingConfig(pageSize = 20),
            pagingSourceFactory = { com.thehotelmedia.android.pagination.collaboration.CollaborationUsersPagingSource(search, individualRepo) }
        ).liveData.cachedIn(viewModelScope)
    }


    //LikeComments
    private val _userProfileByIdResult = MutableLiveData<UserProfileModel>()
    val userProfileByIdResult: LiveData<UserProfileModel> = _userProfileByIdResult
    fun getUserProfileById(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _loading.postValue(true)
            try {
                val response = individualRepo.getUserProfileById(id)
                if (response.isSuccessful) {
//                    val res = response.body()
//                    toastMessageLiveData.postValue(res?.message)
                    _userProfileByIdResult.postValue(response.body())
                    Log.wtf(tag, response.body().toString())
                    _loading.postValue(false)
                } else {
                    Log.wtf(tag + "ELSE", response.message().toString())
                    toastMessageLiveData.postValue(response.message())
                    _loading.postValue(false)
                }

            } catch (t: Throwable) {
                _loading.postValue(false)
                toastMessageLiveData.postValue(t.message)
                Log.wtf(tag + "ERROR", t.message.toString())
            }
        }
    }

    // Get Business Profile by Direct ID (public endpoint)
    // Falls back to user profile endpoint if business profile is not found
    fun getBusinessProfileById(businessProfileId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _loading.postValue(true)
            try {
                val response = individualRepo.getBusinessProfileById(businessProfileId)
                if (response.isSuccessful && response.body()?.status == true) {
                    _userProfileByIdResult.postValue(response.body())
                    Log.wtf(tag, response.body().toString())
                    _loading.postValue(false)
                } else {
                    // If business profile endpoint fails, fall back to user profile endpoint
                    Log.wtf(tag + "BUSINESS_PROFILE_NOT_FOUND", "Falling back to user profile endpoint")
                    try {
                        val userResponse = individualRepo.getUserProfileById(businessProfileId)
                        if (userResponse.isSuccessful) {
                            _userProfileByIdResult.postValue(userResponse.body())
                            Log.wtf(tag, userResponse.body().toString())
                            _loading.postValue(false)
                        } else {
                            Log.wtf(tag + "ELSE", userResponse.message().toString())
                            toastMessageLiveData.postValue(userResponse.message())
                            _loading.postValue(false)
                        }
                    } catch (userException: Throwable) {
                        _loading.postValue(false)
                        toastMessageLiveData.postValue(userException.message)
                        Log.wtf(tag + "ERROR", userException.message.toString())
                    }
                }

            } catch (t: Throwable) {
                // If business endpoint throws an exception, fall back to user endpoint
                Log.wtf(tag + "BUSINESS_PROFILE_ERROR", "Falling back to user profile endpoint: ${t.message}")
                try {
                    val userResponse = individualRepo.getUserProfileById(businessProfileId)
                    if (userResponse.isSuccessful) {
                        _userProfileByIdResult.postValue(userResponse.body())
                        Log.wtf(tag, userResponse.body().toString())
                        _loading.postValue(false)
                    } else {
                        _loading.postValue(false)
                        toastMessageLiveData.postValue(userResponse.message())
                        Log.wtf(tag + "ELSE", userResponse.message().toString())
                    }
                } catch (userException: Throwable) {
                    _loading.postValue(false)
                    toastMessageLiveData.postValue(userException.message)
                    Log.wtf(tag + "ERROR", userException.message.toString())
                }
            }
        }
    }



    fun getNotification(): LiveData<PagingData<NotificationData>> {
        return Pager(
            config = PagingConfig(pageSize = 20),
            pagingSourceFactory = { NotificationPagingSource( individualRepo) }
        ).liveData.cachedIn(viewModelScope)
    }

    fun getSuggestion(): LiveData<PagingData<SuggestionData>> {
        return Pager(
            config = PagingConfig(pageSize = 20),
            pagingSourceFactory = { SuggestionPagingSource( individualRepo) }
        ).liveData.cachedIn(viewModelScope)
    }


    fun getFollowerData(userId : String): LiveData<PagingData<FollowFollowingData>> {
        return Pager(
            config = PagingConfig(pageSize = 20),
            pagingSourceFactory = { FollowerPagingSource( userId, individualRepo) }
        ).liveData.cachedIn(viewModelScope)
    }

    fun getFollowingData(userId : String): LiveData<PagingData<FollowFollowingData>> {
        return Pager(
            config = PagingConfig(pageSize = 20),
            pagingSourceFactory = { FollowingPagingSource( userId, individualRepo) }
        ).liveData.cachedIn(viewModelScope)
    }

    fun getBlockUserData(): LiveData<PagingData<FollowFollowingData>> {
        return Pager(
            config = PagingConfig(pageSize = 20),
            pagingSourceFactory = { BlockedUserPagingSource( individualRepo) }
        ).liveData.cachedIn(viewModelScope)
    }



    //AcceptFollow
    private val _acceptFollowResult = MutableLiveData<FollowActionModal>()
    val acceptFollowResult: LiveData<FollowActionModal> = _acceptFollowResult
    fun acceptRequest(connectionId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _loading.postValue(true)
            try {
                val response = individualRepo.acceptRequest(connectionId)
                if (response.isSuccessful) {
//                    val res = response.body()
//                    toastMessageLiveData.postValue(res?.message)
                    _acceptFollowResult.postValue(response.body())
                    Log.wtf(tag, response.body().toString())
                    _loading.postValue(false)
                } else {
                    Log.wtf(tag + "ELSE", response.message().toString())
                    toastMessageLiveData.postValue(response.message())
                    _loading.postValue(false)
                }

            } catch (t: Throwable) {
                _loading.postValue(false)
                toastMessageLiveData.postValue(t.message)
                Log.wtf(tag + "ERROR", t.message.toString())
            }
        }
    }

    //DeclineRequest
    private val _declineRequestResult = MutableLiveData<FollowActionModal>()
    val declineRequestResult: LiveData<FollowActionModal> = _declineRequestResult
    fun declineRequest(connectionId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _loading.postValue(true)
            try {
                val response = individualRepo.declineRequest(connectionId)
                if (response.isSuccessful) {
//                    val res = response.body()
//                    toastMessageLiveData.postValue(res?.message)
                    _declineRequestResult.postValue(response.body())
                    Log.wtf(tag, response.body().toString())
                    _loading.postValue(false)
                } else {
                    Log.wtf(tag + "ELSE", response.message().toString())
                    toastMessageLiveData.postValue(response.message())
                    _loading.postValue(false)
                }

            } catch (t: Throwable) {
                _loading.postValue(false)
                toastMessageLiveData.postValue(t.message)
                Log.wtf(tag + "ERROR", t.message.toString())
            }
        }
    }


    //FollowBack
    private val _followBackResult = MutableLiveData<FollowActionModal>()
    val followBackResult: LiveData<FollowActionModal> = _followBackResult
    fun followBack(connectionId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _loading.postValue(true)
            try {
                val response = individualRepo.followBack(connectionId)
                if (response.isSuccessful) {
//                    val res = response.body()
//                    toastMessageLiveData.postValue(res?.message)
                    _followBackResult.postValue(response.body())
                    Log.wtf(tag, response.body().toString())
                    _loading.postValue(false)
                } else {
                    Log.wtf(tag + "ELSE", response.message().toString())
                    toastMessageLiveData.postValue(response.message())
                    _loading.postValue(false)
                }

            } catch (t: Throwable) {
                _loading.postValue(false)
                toastMessageLiveData.postValue(t.message)
                Log.wtf(tag + "ERROR", t.message.toString())
            }
        }
    }

    //FollowUser
    private val _followUserResult = MutableLiveData<FollowUserModal>()
    val followUserResult: LiveData<FollowUserModal> = _followUserResult
    fun followUser(userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _loading.postValue(true)
            try {
                val response = individualRepo.followUser(userId)
                if (response.isSuccessful) {
//                    val res = response.body()
//                    toastMessageLiveData.postValue(res?.message)
                    _followUserResult.postValue(response.body())
                    Log.wtf(tag, response.body().toString())
                    _loading.postValue(false)
                } else {
                    Log.wtf(tag + "ELSE", response.message().toString())
                    toastMessageLiveData.postValue(response.message())
                    _loading.postValue(false)
                }

            } catch (t: Throwable) {
                _loading.postValue(false)
                toastMessageLiveData.postValue(t.message)
                Log.wtf(tag + "ERROR", t.message.toString())
            }
        }
    }

    //UnFollowUser
    private val _unFollowUserResult = MutableLiveData<UnfollowModel>()
    val unFollowUserResult: LiveData<UnfollowModel> = _unFollowUserResult
    fun unFollowUser(userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _loading.postValue(true)
            try {
                val response = individualRepo.unFollowUser(userId)
                if (response.isSuccessful) {
//                    val res = response.body()
//                    toastMessageLiveData.postValue(res?.message)
                    _unFollowUserResult.postValue(response.body())
                    Log.wtf(tag, response.body().toString())
                    _loading.postValue(false)
                } else {
                    Log.wtf(tag + "ELSE", response.message().toString())
                    toastMessageLiveData.postValue(response.message())
                    _loading.postValue(false)
                }

            } catch (t: Throwable) {
                _loading.postValue(false)
                toastMessageLiveData.postValue(t.message)
                Log.wtf(tag + "ERROR", t.message.toString())
            }
        }
    }


    fun getSearchData(query : String,type : String,businessTypeID : List<String>,initialKm :String,lat :Double,lng :Double): LiveData<PagingData<SearchData>> {
        var currentLat = 0.0
        var currentLng = 0.0
        if (query.isNotEmpty()){
            currentLat = 0.0
            currentLng = 0.0
        }else{
            currentLat = lat
            currentLng = lng
        }
        return Pager(
            config = PagingConfig(pageSize = 20),
            pagingSourceFactory = { SearchPagingSource( query,type,businessTypeID,initialKm,currentLat,currentLng, individualRepo) }
        ).liveData.cachedIn(viewModelScope)
    }


    fun getPostsData(userId : String): LiveData<PagingData<Data>> {
        return Pager(
            config = PagingConfig(pageSize = 20),
            pagingSourceFactory = { PostsPagingSource( userId, individualRepo) }
        ).liveData.cachedIn(viewModelScope)
    }

    fun getReviewData(userId : String): LiveData<PagingData<SearchData>> {
        return Pager(
            config = PagingConfig(pageSize = 20),
            pagingSourceFactory = { ReviewPagingSource( userId, individualRepo) }
        ).liveData.cachedIn(viewModelScope)
    }


    fun getTransactionData(): LiveData<PagingData<TransactionData>> {
        return Pager(
            config = PagingConfig(pageSize = 20),
            pagingSourceFactory = { TransactionPagingSource( individualRepo) }
        ).liveData.cachedIn(viewModelScope)
    }

    //Faq
    fun getFaq(search: String,type: String,): LiveData<PagingData<FAQsData>> {
        return Pager(
            config = PagingConfig(pageSize = 20),
            pagingSourceFactory = { FAQPagingSource(search,type, individualRepo) }
        ).liveData.cachedIn(viewModelScope)

    }


    //ContactUs
    private val _contactUsResult = MutableLiveData<ContactUsModal>()
    val contactUsResult: LiveData<ContactUsModal> = _contactUsResult
    fun contactUs(name: String,email: String,message: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _loading.postValue(true)
            try {
                val response = individualRepo.contactUs(name,email,message)
                if (response.isSuccessful) {
//                    val res = response.body()
//                    toastMessageLiveData.postValue(res?.message)
                    _contactUsResult.postValue(response.body())
                    Log.wtf(tag, response.body().toString())
                    _loading.postValue(false)
                } else {
                    Log.wtf(tag + "ELSE", response.message().toString())
                    toastMessageLiveData.postValue(response.message())
                    _loading.postValue(false)
                }

            } catch (t: Throwable) {
                _loading.postValue(false)
                toastMessageLiveData.postValue(t.message)
                Log.wtf(tag + "ERROR", t.message.toString())
            }
        }
    }


    //SubscriptionsData
    private val _subscriptionsResult = MutableLiveData<SubscriptionsModal>()
    val subscriptionsResult: LiveData<SubscriptionsModal> = _subscriptionsResult
    fun getSubscriptionsData() {
        viewModelScope.launch(Dispatchers.IO) {
            _loading.postValue(true)
            try {
                val response = individualRepo.getSubscriptionsData()
                if (response.isSuccessful) {
//                    val res = response.body()
//                    toastMessageLiveData.postValue(res?.message)
                    _subscriptionsResult.postValue(response.body())
                    Log.wtf(tag, response.body().toString())
                    _loading.postValue(false)
                } else {
                    Log.wtf(tag + "ELSE", response.message().toString())
                    toastMessageLiveData.postValue(response.message())
                    _loading.postValue(false)
                }

            } catch (t: Throwable) {
                _loading.postValue(false)
                toastMessageLiveData.postValue(t.message)
                Log.wtf(tag + "ERROR", t.message.toString())
            }
        }
    }


    //Cancel Subscriptions
    private val _cancelSubscriptionsResult = MutableLiveData<CancelSubscriptions>()
    val cancelSubscriptionsResult: LiveData<CancelSubscriptions> = _cancelSubscriptionsResult
    fun cancelSubscriptions() {
        viewModelScope.launch(Dispatchers.IO) {
//            _loading.postValue(true)
            try {
                val response = individualRepo.cancelSubscriptions()
                if (response.isSuccessful) {
//                    val res = response.body()
//                    toastMessageLiveData.postValue(res?.message)
                    _cancelSubscriptionsResult.postValue(response.body())
                    Log.wtf(tag, response.body().toString())
//                    _loading.postValue(false)
                } else {
                    Log.wtf(tag + "ELSE", response.message().toString())
                    toastMessageLiveData.postValue(response.message())
//                    _loading.postValue(false)
                }

            } catch (t: Throwable) {
//                _loading.postValue(false)
                toastMessageLiveData.postValue(t.message)
                Log.wtf(tag + "ERROR", t.message.toString())
            }
        }
    }


    //Show all Posts
    private val _getAllPostsResult = MutableLiveData<ViewPostEventModal>()
    val getAllPostsResult: LiveData<ViewPostEventModal> = _getAllPostsResult
    fun getAllPosts(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _loading.postValue(true)
            try {
                val response = individualRepo.getAllPosts(id)
                if (response.isSuccessful) {
//                    val res = response.body()
//                    toastMessageLiveData.postValue(res?.message)
                    _getAllPostsResult.postValue(response.body())
                    Log.wtf(tag, response.body().toString())
                    _loading.postValue(false)
                } else {
                    Log.wtf(tag + "ELSE", response.message().toString())
                    toastMessageLiveData.postValue(response.message())
                    _loading.postValue(false)
                }

            } catch (t: Throwable) {
                _loading.postValue(false)
                toastMessageLiveData.postValue(t.message)
                Log.wtf(tag + "ERROR", t.message.toString())
            }
        }
    }

    //Show single Posts
    private val _getSinglePostsResult = MutableLiveData<SharePostModal>()
    val getSinglePostsResult: LiveData<SharePostModal> = _getSinglePostsResult
    fun getSinglePosts(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _loading.postValue(true)
            try {
                val response = individualRepo.getSinglePosts(id)
                if (response.isSuccessful) {
//                    val res = response.body()
//                    toastMessageLiveData.postValue(res?.message)
                    _getSinglePostsResult.postValue(response.body())
                    Log.wtf(tag, response.body().toString())
                    _loading.postValue(false)
                } else {
                    Log.wtf(tag + "ELSE", response.message().toString())
                    toastMessageLiveData.postValue(response.message())
                    _loading.postValue(false)
                }

            } catch (t: Throwable) {
                _loading.postValue(false)
                toastMessageLiveData.postValue(t.message)
                Log.wtf(tag + "ERROR", t.message.toString())
            }
        }
    }

    //Visit Website
    private val _visitWebsiteResult = MutableLiveData<WebsiteRedirectModal>()
    val visitWebsiteResult: LiveData<WebsiteRedirectModal> = _visitWebsiteResult
    fun visitWebsite(businessProfileID: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _loading.postValue(true)
            try {
                val response = individualRepo.visitWebsite(businessProfileID)
                if (response.isSuccessful) {
//                    val res = response.body()
//                    toastMessageLiveData.postValue(res?.message)
                    _visitWebsiteResult.postValue(response.body())
                    Log.wtf(tag, response.body().toString())
                    _loading.postValue(false)
                } else {
                    Log.wtf(tag + "ELSE", "visitWebsite ${response.message().toString()}")
                    toastMessageLiveData.postValue(response.message())
                    _loading.postValue(false)
                }

            } catch (t: Throwable) {
                _loading.postValue(false)
                toastMessageLiveData.postValue(t.message)
                Log.wtf(tag + "ERROR", t.message.toString())
            }
        }
    }

    //Block User
    private val _blockUserResult = MutableLiveData<BlockUserModel>()
    val blockUserResult: LiveData<BlockUserModel> = _blockUserResult
    fun blockUser(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
//            _loading.postValue(true)
            try {
                val response = individualRepo.blockUser(id)
                if (response.isSuccessful) {
//                    val res = response.body()
//                    toastMessageLiveData.postValue(res?.message)
                    _blockUserResult.postValue(response.body())
                    Log.wtf(tag, response.body().toString())
//                    _loading.postValue(false)
                } else {
                    Log.wtf(tag + "ELSE", response.message().toString())
                    toastMessageLiveData.postValue(response.message())
//                    _loading.postValue(false)
                }

            } catch (t: Throwable) {
//                _loading.postValue(false)
                toastMessageLiveData.postValue(t.message)
                Log.wtf(tag + "ERROR", t.message.toString())
            }
        }
    }



    //Get Insight Data
    private val _getInsightResult = MutableLiveData<InsightModal>()
    val getInsightResult: LiveData<InsightModal> = _getInsightResult
    fun getInsight(filter : String) {
        viewModelScope.launch(Dispatchers.IO) {
            _loading.postValue(true)
            try {
                val response = individualRepo.getInsight(filter)
                if (response.isSuccessful) {
//                    val res = response.body()
//                    toastMessageLiveData.postValue(res?.message)
                    _getInsightResult.postValue(response.body())
                    Log.wtf(tag, response.body().toString())
                    _loading.postValue(false)
                } else {
                    Log.wtf(tag + "ELSE", "get Insight  ${response.message().toString()}")
                    toastMessageLiveData.postValue(response.message())
                    _loading.postValue(false)
                }

            } catch (t: Throwable) {
                _loading.postValue(false)
                toastMessageLiveData.postValue(t.message)
                Log.wtf(tag + "ERROR", t.message.toString())
            }
        }
    }

    //Account Reach
    private val _accountReachResult = MutableLiveData<AccountReachModal>()
    val accountReachResult: LiveData<AccountReachModal> = _accountReachResult
    fun accountReach(businessProfileID: String) {
        viewModelScope.launch(Dispatchers.IO) {
//            _loading.postValue(true)
            try {
                val response = individualRepo.accountReach(businessProfileID)
                if (response.isSuccessful) {
//                    val res = response.body()
//                    toastMessageLiveData.postValue(res?.message)
                    _accountReachResult.postValue(response.body())
                    Log.wtf(tag, response.body().toString())
//                    _loading.postValue(false)
                } else {
                    Log.wtf(tag + "ELSE", "account reach ${response.message().toString()}")
                    toastMessageLiveData.postValue(response.message())
//                    _loading.postValue(false)
                }

            } catch (t: Throwable) {
//                _loading.postValue(false)
                toastMessageLiveData.postValue(t.message)
                Log.wtf(tag + "ERROR", t.message.toString())
            }
        }
    }

    //Update Address
    private val _updateAddressResult = MutableLiveData<UpdateAddressModal>()
    val updateAddressResult: LiveData<UpdateAddressModal> = _updateAddressResult
    fun updateAddress(street: String,city: String,state: String,zipCode: String,country: String,lat: String,lng: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _loading.postValue(true)
            try {
                val response = individualRepo.updateAddress(street,city,state,zipCode,country,lat,lng)
                if (response.isSuccessful) {
                    val res = response.body()
                    toastMessageLiveData.postValue(res?.message ?: N_A)
                    _updateAddressResult.postValue(response.body())
                    Log.wtf(tag, response.body().toString())
                    _loading.postValue(false)
                } else {
                    Log.wtf(tag + "ELSE", response.message().toString())
                    toastMessageLiveData.postValue(response.message())
                    _loading.postValue(false)
                }

            } catch (t: Throwable) {
                _loading.postValue(false)
                toastMessageLiveData.postValue(t.message)
                Log.wtf(tag + "ERROR", t.message.toString())
            }
        }
    }


    //Delete Story
    private val _deleteStoryResult = MutableLiveData<DeleteStoryModal>()
    val deleteStoryResult: LiveData<DeleteStoryModal> = _deleteStoryResult
    fun deleteStory(storyId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _loading.postValue(true)
            try {
                val response = individualRepo.deleteStory(storyId)
                if (response.isSuccessful) {
//                    val res = response.body()
//                    toastMessageLiveData.postValue(res?.message)
                    _deleteStoryResult.postValue(response.body())
                    Log.wtf(tag, response.body().toString())
                    _loading.postValue(false)
                } else {
                    Log.wtf(tag + "ELSE", response.message().toString())
                    toastMessageLiveData.postValue(response.message())
                    _loading.postValue(false)
                }

            } catch (t: Throwable) {
                _loading.postValue(false)
                toastMessageLiveData.postValue(t.message)
                Log.wtf(tag + "ERROR", t.message.toString())
            }
        }
    }

    //View Story
    private val _viewStoryResult = MutableLiveData<StoryActionModal>()
    val viewStoryResult: LiveData<StoryActionModal> = _viewStoryResult
    fun viewStory(storyId: String) {
        viewModelScope.launch(Dispatchers.IO) {
//            _loading.postValue(true)
            try {
                val response = individualRepo.viewStory(storyId)
                if (response.isSuccessful) {
//                    val res = response.body()
//                    toastMessageLiveData.postValue(res?.message)
                    _viewStoryResult.postValue(response.body())
                    Log.wtf(tag, response.body().toString())
//                    _loading.postValue(false)
                } else {
                    Log.wtf(tag + "ELSE", response.message().toString())
                    toastMessageLiveData.postValue(response.message())
//                    _loading.postValue(false)
                }

            } catch (t: Throwable) {
//                _loading.postValue(false)
                toastMessageLiveData.postValue(t.message)
                Log.wtf(tag + "ERROR", t.message.toString())
            }
        }
    }

    //Like Story
    private val _likeStoryResult = MutableLiveData<StoryActionModal>()
    val likeStoryResult: LiveData<StoryActionModal> = _likeStoryResult
    fun likeStory(storyId: String) {
        viewModelScope.launch(Dispatchers.IO) {
//            _loading.postValue(true)
            try {
                val response = individualRepo.likeStory(storyId)
                if (response.isSuccessful) {
//                    val res = response.body()
//                    toastMessageLiveData.postValue(res?.message)
                    _likeStoryResult.postValue(response.body())
                    Log.wtf(tag, response.body().toString())
//                    _loading.postValue(false)
                } else {
                    Log.wtf(tag + "ELSE", response.message().toString())
                    toastMessageLiveData.postValue(response.message())
//                    _loading.postValue(false)
                }

            } catch (t: Throwable) {
//                _loading.postValue(false)
                toastMessageLiveData.postValue(t.message)
                Log.wtf(tag + "ERROR", t.message.toString())
            }
        }
    }


    //Likes
    fun getLikes(storyId: String): LiveData<PagingData<LikeData>> {
        return Pager(
            config = PagingConfig(pageSize = 20),
            pagingSourceFactory = { LikesPagingSource(storyId, individualRepo) }
        ).liveData.cachedIn(viewModelScope)
    }
    //Viewers
    fun getViewers(storyId: String): LiveData<PagingData<LikeData>> {
        return Pager(
            config = PagingConfig(pageSize = 20),
            pagingSourceFactory = { ViewersPagingSource(storyId, individualRepo) }
        ).liveData.cachedIn(viewModelScope)
    }


    //Like Story
    private val _sendMediaResult = MutableLiveData<SendMediaModal>()
    val sendMediaResult: LiveData<SendMediaModal> = _sendMediaResult
    fun sendMediaMessage(username: String, message: String, messageType: String, media: File) {
        viewModelScope.launch(Dispatchers.IO) {
            _loading.postValue(true)
            try {
                val response = individualRepo.sendMediaMessage(username,message, messageType, media)
                if (response.isSuccessful) {
//                    val res = response.body()
//                    toastMessageLiveData.postValue(res?.message)
                    _sendMediaResult.postValue(response.body())
                    Log.wtf(tag, response.body().toString())
                    _loading.postValue(false)
                } else {
                    Log.wtf(tag + "ELSE", response.toString())
                    toastMessageLiveData.postValue(response.message())
                    _loading.postValue(false)
                }

            } catch (t: Throwable) {
                _loading.postValue(false)
                toastMessageLiveData.postValue(t.message)
                Log.wtf(tag + "ERROR", t.message.toString())
            }
        }
    }



    //Report User
    private val _reportUserResult = MutableLiveData<ReportUserModal>()
    val reportUserResult: LiveData<ReportUserModal> = _reportUserResult
    fun reportUser(userid: String,reason: String) {
        viewModelScope.launch(Dispatchers.IO) {
//            _loading.postValue(true)
            try {
                val response = individualRepo.reportUser(userid,reason)
                if (response.isSuccessful) {
                    val res = response.body()
                    toastMessageLiveData.postValue(res?.message ?: N_A)
                    _reportUserResult.postValue(response.body())
                    Log.wtf(tag, response.body().toString())
//                    _loading.postValue(false)
                } else {
                    Log.wtf(tag + "ELSE", response.message().toString())
                    toastMessageLiveData.postValue(response.message())
//                    _loading.postValue(false)
                }

            } catch (t: Throwable) {
//                _loading.postValue(false)
                toastMessageLiveData.postValue(t.message)
                Log.wtf(tag + "ERROR", t.message.toString())
            }
        }
    }

    //Report Post
    private val _reportPostsResult = MutableLiveData<ReportUserModal>()
    val reportPostsResult: LiveData<ReportUserModal> = _reportPostsResult
    fun reportPosts(postId: String,reason: String) {
        viewModelScope.launch(Dispatchers.IO) {
//            _loading.postValue(true)
            try {
                val response = individualRepo.reportPosts(postId,reason)
                if (response.isSuccessful) {
                    val res = response.body()
                    reportToastMessageLiveData.postValue(res?.message ?: "")
                    _reportPostsResult.postValue(response.body())
                    Log.wtf(tag, response.body().toString())
//                    _loading.postValue(false)
                } else {
                    Log.wtf(tag + "ELSE", response.message().toString())
//                    reportToastMessageLiveData.postValue(response.message())
//                    _loading.postValue(false)
                }

            } catch (t: Throwable) {
//                _loading.postValue(false)
                reportToastMessageLiveData.postValue(t.message)
                Log.wtf(tag + "ERROR", t.message.toString())
            }
        }
    }

    //Report Comment
    private val _reportCommentResult = MutableLiveData<ReportUserModal>()
    val reportCommentResult: LiveData<ReportUserModal> = _reportCommentResult
    fun reportComment(commentId: String,reason: String) {
        viewModelScope.launch(Dispatchers.IO) {
//            _loading.postValue(true)
            try {
                val response = individualRepo.reportComment(commentId,reason)
                if (response.isSuccessful) {
                    val res = response.body()
                    reportToastMessageLiveData.postValue(res?.message ?: "")
                    _reportCommentResult.postValue(response.body())
                    Log.wtf(tag, response.body().toString())
//                    _loading.postValue(false)
                } else {
                    Log.wtf(tag + "ELSE", response.message().toString())
//                    toastMessageLiveData.postValue(response.message())
//                    _loading.postValue(false)
                }

            } catch (t: Throwable) {
//                _loading.postValue(false)
                reportToastMessageLiveData.postValue(t.message)
                Log.wtf(tag + "ERROR", t.message.toString())
            }
        }
    }

    //Delete Comment
    private val _deleteCommentResult = MutableLiveData<DeleteModal>()
    val deleteCommentResult: LiveData<DeleteModal> = _deleteCommentResult
    fun deleteComment(commentId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _loading.postValue(true)
            try {
                val response = individualRepo.deleteComment(commentId)
                if (response.isSuccessful) {
                    val res = response.body()
                    toastMessageLiveData.postValue(res?.message ?: N_A)
                    _deleteCommentResult.postValue(response.body())
                    Log.wtf(tag, response.body().toString())
                    _loading.postValue(false)
                } else {
                    Log.wtf(tag + "ELSE", response.message().toString())
                    toastMessageLiveData.postValue(response.message())
                    _loading.postValue(false)
                }

            } catch (t: Throwable) {
                _loading.postValue(false)
                toastMessageLiveData.postValue(t.message)
                Log.wtf(tag + "ERROR", t.message.toString())
            }
        }
    }

    //Shared Post
    private val _getSharedPostsResult = MutableLiveData<SharePostModal>()
    val getSharedPostsResult: LiveData<SharePostModal> = _getSharedPostsResult
    fun getSharedPosts(postId: String,userID: String) {
        viewModelScope.launch(Dispatchers.IO) {
//            _loading.postValue(true)
            try {
                val response = individualRepo.getSharedPosts(postId,userID)
                if (response.isSuccessful) {
//                    val res = response.body()
//                    toastMessageLiveData.postValue(res?.message)
                    _getSharedPostsResult.postValue(response.body())
                    Log.wtf(tag, response.body().toString())
                    _loading.postValue(false)
                } else {
                    Log.wtf(tag + "ELSE", response.message().toString())
                    toastMessageLiveData.postValue(response.message())
                    _loading.postValue(false)
                }

            } catch (t: Throwable) {
                _loading.postValue(false)
                toastMessageLiveData.postValue(t.message)
                Log.wtf(tag + "ERROR", t.message.toString())
            }
        }
    }

    //Saved Business Documents
    private val _getSavedDocumentResult = MutableLiveData<GetBusinessDocumentsModal>()
    val getSavedDocumentResult: LiveData<GetBusinessDocumentsModal> = _getSavedDocumentResult
    fun getSavedBusinessDocuments() {
        viewModelScope.launch(Dispatchers.IO) {
            _loading.postValue(true)
            try {
                val response = individualRepo.getSavedDocuments()
                if (response.isSuccessful) {
//                    val res = response.body()
//                    toastMessageLiveData.postValue(res?.message)
                    _getSavedDocumentResult.postValue(response.body())
                    Log.wtf(tag, response.body().toString())
                    _loading.postValue(false)
                } else {
                    Log.wtf(tag + "ELSE", response.message().toString())
                    toastMessageLiveData.postValue(response.message())
                    _loading.postValue(false)
                }

            } catch (t: Throwable) {
                _loading.postValue(false)
                toastMessageLiveData.postValue(t.message)
                Log.wtf(tag + "ERROR", t.message.toString())
            }
        }
    }


    //LikeComments
    private val _notificationStatusResult = MutableLiveData<NotificationStatusModal>()
    val notificationStatusResult: LiveData<NotificationStatusModal> = _notificationStatusResult
    fun getNotificationStatus() {
        viewModelScope.launch(Dispatchers.IO) {
//            _loading.postValue(true)
            try {
                val response = individualRepo.getNotificationStatus()
                if (response.isSuccessful) {
//                    val res = response.body()
//                    toastMessageLiveData.postValue(res?.message)
                    _notificationStatusResult.postValue(response.body())
                    Log.wtf(tag, response.body().toString())
//                    _loading.postValue(false)
                } else {
                    Log.wtf(tag + "ELSE", response.message().toString())
                    toastMessageLiveData.postValue(response.message())
//                    _loading.postValue(false)
                }

            } catch (t: Throwable) {
//                _loading.postValue(false)
                toastMessageLiveData.postValue(t.message)
                Log.wtf(tag + "ERROR", t.message.toString())
            }
        }
    }





    //Booking CheckIn
    private val _bookingCheckInResult = MutableLiveData<BookingCheckInModal>()
    val bookingCheckInResult: LiveData<BookingCheckInModal> = _bookingCheckInResult

    // Rooms list (all rooms for a business profile)
    private val _roomsListResult = MutableLiveData<RoomsListModal>()
    val roomsListResult: LiveData<RoomsListModal> = _roomsListResult

    // Canonical room cards for booking UI (hydrated with images/details from backend)
    private val _roomsForHotelResult = MutableLiveData<ArrayList<com.thehotelmedia.android.modals.booking.checkIn.AvailableRooms>>()
    val roomsForHotelResult: LiveData<ArrayList<com.thehotelmedia.android.modals.booking.checkIn.AvailableRooms>> = _roomsForHotelResult

    fun fetchRoomsByBusinessProfile(businessProfileID: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d(tag, "getRoomsByBusinessProfile request => businessProfileID=$businessProfileID")
                val response = individualRepo.getRoomsByBusinessProfile(businessProfileID)
                if (response.isSuccessful) {
                    _roomsListResult.postValue(response.body())
                } else {
                    toastMessageLiveData.postValue(response.message())
                }
            } catch (t: Throwable) {
                toastMessageLiveData.postValue(t.message)
            }
        }
    }

    /**
     * Fetch all room types for a hotel (dashboard parity), but hydrate each room with full details so
     * the booking UI shows real images (not placeholders) and avoids duplicate rows.
     */
    fun fetchRoomsForHotel(businessProfileID: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d(tag, "fetchRoomsForHotel request => businessProfileID=$businessProfileID")
                val listRes = individualRepo.getRoomsByBusinessProfile(businessProfileID)
                if (!listRes.isSuccessful) {
                    toastMessageLiveData.postValue(listRes.message())
                    return@launch
                }

                val listBody = listRes.body()
                val roomsFromList = listBody?.rooms().orEmpty()

                // Some backend deployments ignore the businessProfileID query and return all rooms.
                // Filter strictly on businessProfileID to avoid showing rooms from other hotels.
                val roomsForThisHotel = roomsFromList.filter { it.businessProfileID == businessProfileID }
                    .ifEmpty { roomsFromList } // fallback if backend doesn't include businessProfileID in list payload

                // Deduplicate: some backends expand inventory into multiple rows (same title). We want unique room types.
                val deduped = roomsForThisHotel.distinctBy { it.Id ?: it.title ?: "" }
                    .ifEmpty { roomsForThisHotel.distinctBy { it.title ?: it.Id ?: "" } }

                Log.d(tag, "fetchRoomsForHotel rooms from list=${roomsFromList.size}, filtered=${roomsForThisHotel.size}, deduped=${deduped.size}")

                val hydrated = arrayListOf<com.thehotelmedia.android.modals.booking.checkIn.AvailableRooms>()
                for (room in deduped) {
                    val roomId = room.Id ?: continue
                    try {
                        val detailsRes = individualRepo.fetchRoomDetails(roomId)
                        val details = if (detailsRes.isSuccessful) detailsRes.body()?.data else null
                        val finalRoom = details ?: room
                        hydrated.add(
                            com.thehotelmedia.android.modals.booking.checkIn.AvailableRooms(
                                Id = finalRoom.Id,
                                bedType = finalRoom.bedType,
                                adults = finalRoom.adults,
                                children = finalRoom.children,
                                maxOccupancy = finalRoom.maxOccupancy,
                                availability = finalRoom.availability,
                                amenities = finalRoom.amenities,
                                title = finalRoom.title,
                                description = finalRoom.description,
                                pricePerNight = finalRoom.pricePerNight,
                                currency = finalRoom.currency,
                                mealPlan = finalRoom.mealPlan,
                                cover = finalRoom.cover?.let { c ->
                                    com.thehotelmedia.android.modals.booking.checkIn.Cover(
                                        Id = c.Id,
                                        isCoverImage = c.isCoverImage,
                                        sourceUrl = c.sourceUrl,
                                        thumbnailUrl = c.thumbnailUrl
                                    )
                                },
                                roomImagesRef = ArrayList(
                                    finalRoom.roomImagesRef.map { img ->
                                        com.thehotelmedia.android.modals.booking.checkIn.Cover(
                                            Id = img.Id,
                                            isCoverImage = img.isCoverImage,
                                            sourceUrl = img.sourceUrl,
                                            thumbnailUrl = img.thumbnailUrl
                                        )
                                    }
                                ),
                                amenitiesRef = ArrayList(
                                    finalRoom.amenitiesRef.map { a ->
                                        com.thehotelmedia.android.modals.booking.checkIn.AmenitiesRef(
                                            Id = a.Id,
                                            name = a.name,
                                            category = a.category
                                        )
                                    }
                                )
                            )
                        )
                    } catch (e: Exception) {
                        Log.w(tag, "fetchRoomsForHotel fetchRoomDetails failed roomId=$roomId : ${e.message}")
                    }
                }

                _roomsForHotelResult.postValue(hydrated)
            } catch (t: Throwable) {
                toastMessageLiveData.postValue(t.message)
            }
        }
    }
    fun bookingCheckIn(businessProfileID: String,checkInDate: String,checkOutDate: String,adultsCount: Int,childrenCount: Int,childrenAges: List<Int>,isTravellingWithPet : Boolean) {

        viewModelScope.launch(Dispatchers.IO) {
            _loading.postValue(true)
            try {
                Log.d(
                    tag,
                    "bookingCheckIn request => businessProfileID=$businessProfileID, checkIn=$checkInDate, checkOut=$checkOutDate, adults=$adultsCount, children=$childrenCount, childrenAges=$childrenAges, pet=$isTravellingWithPet"
                )
                val response = individualRepo.bookingCheckIn(businessProfileID,checkInDate,checkOutDate,adultsCount, childrenCount,childrenAges,isTravellingWithPet)
                if (response.isSuccessful) {
                    val body = response.body()

                    // Some backend versions return `data.availability` but keep `data.availableRooms` empty.
                    // Our UI renders `availableRooms`, so we hydrate it by fetching full room details.
                    val availabilityIds = body?.data?.availability
                        ?.mapNotNull { it.id }
                        ?.distinct()
                        .orEmpty()

                    val hasRoomCards = !body?.data?.availableRooms.isNullOrEmpty()

                    if (!hasRoomCards && availabilityIds.isNotEmpty()) {
                        val hydratedRooms = arrayListOf<com.thehotelmedia.android.modals.booking.checkIn.AvailableRooms>()
                        for (roomId in availabilityIds) {
                            try {
                                val roomRes = individualRepo.fetchRoomDetails(roomId)
                                if (roomRes.isSuccessful) {
                                    val room = roomRes.body()?.data
                                    if (room != null) {
                                        hydratedRooms.add(
                                            com.thehotelmedia.android.modals.booking.checkIn.AvailableRooms(
                                                Id = room.Id,
                                                bedType = room.bedType,
                                                adults = room.adults,
                                                children = room.children,
                                                maxOccupancy = room.maxOccupancy,
                                                availability = room.availability,
                                                amenities = room.amenities,
                                                title = room.title,
                                                description = room.description,
                                                pricePerNight = room.pricePerNight,
                                                currency = room.currency,
                                                mealPlan = room.mealPlan,
                                                cover = room.cover?.let { c ->
                                                    com.thehotelmedia.android.modals.booking.checkIn.Cover(
                                                        Id = c.Id,
                                                        isCoverImage = c.isCoverImage,
                                                        sourceUrl = c.sourceUrl,
                                                        thumbnailUrl = c.thumbnailUrl
                                                    )
                                                },
                                                roomImagesRef = ArrayList(
                                                    room.roomImagesRef.map { img ->
                                                        com.thehotelmedia.android.modals.booking.checkIn.Cover(
                                                            Id = img.Id,
                                                            isCoverImage = img.isCoverImage,
                                                            sourceUrl = img.sourceUrl,
                                                            thumbnailUrl = img.thumbnailUrl
                                                        )
                                                    }
                                                ),
                                                amenitiesRef = ArrayList(
                                                    room.amenitiesRef.map { a ->
                                                        com.thehotelmedia.android.modals.booking.checkIn.AmenitiesRef(
                                                            Id = a.Id,
                                                            name = a.name,
                                                            category = a.category
                                                        )
                                                    }
                                                )
                                            )
                                        )
                                    }
                                } else {
                                    Log.w(tag, "fetchRoomDetails failed for roomId=$roomId : ${roomRes.message()}")
                                }
                            } catch (e: Exception) {
                                Log.w(tag, "fetchRoomDetails exception for roomId=$roomId : ${e.message}")
                            }
                        }

                        body?.data?.availableRooms?.clear()
                        body?.data?.availableRooms?.addAll(hydratedRooms)
                    }

                    _bookingCheckInResult.postValue(body)
                    Log.wtf(tag, body.toString())
                    _loading.postValue(false)
                } else {
                    Log.wtf(tag + "ELSE", response.message().toString())
                    toastMessageLiveData.postValue(response.message())
                    _loading.postValue(false)
                }

            } catch (t: Throwable) {
                _loading.postValue(false)
                toastMessageLiveData.postValue(t.message)
                Log.wtf(tag + "ERROR", t.message.toString())
            }
        }
    }

    //Room Details
    private val _roomDetailsResult = MutableLiveData<RoomDetailsModal>()
    val roomDetailsResult: LiveData<RoomDetailsModal> = _roomDetailsResult
    fun fetchRoomDetails(roomId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _loading.postValue(true)
            try {
                val response = individualRepo.fetchRoomDetails(roomId)
                if (response.isSuccessful) {
//                    val res = response.body()
//                    toastMessageLiveData.postValue(res?.message)
                    _roomDetailsResult.postValue(response.body())
                    Log.wtf(tag, response.body().toString())
                    _loading.postValue(false)
                } else {
                    Log.wtf(tag + "ELSE", response.message().toString())
                    toastMessageLiveData.postValue(response.message())
                    _loading.postValue(false)
                }

            } catch (t: Throwable) {
                _loading.postValue(false)
                toastMessageLiveData.postValue(t.message)
                Log.wtf(tag + "ERROR", t.message.toString())
            }
        }
    }

    //Booking CheckOut
    private val _bookingCheckOutResult = MutableLiveData<BookingCheckOutModal>()
    val bookingCheckOutResult: LiveData<BookingCheckOutModal> = _bookingCheckOutResult
    fun bookingCheckOut(bookingId: String,roomId: String,quantity: Int,promoCode: String,price: Double) {

        viewModelScope.launch(Dispatchers.IO) {
            _loading.postValue(true)
            try {
                val response = individualRepo.bookingCheckOut(bookingId,roomId,quantity,promoCode,price)
                if (response.isSuccessful) {
//                    val res = response.body()
//                    toastMessageLiveData.postValue(res?.message)
                    _bookingCheckOutResult.postValue(response.body())
                    Log.wtf(tag, response.body().toString())
                    _loading.postValue(false)
                } else {
                    Log.wtf(tag + "ELSE", response.message().toString())
                    println("$tag $response")
                    toastMessageLiveData.postValue(response.message())
                    _loading.postValue(false)
                }

            } catch (t: Throwable) {
                _loading.postValue(false)
                toastMessageLiveData.postValue(t.message)
                Log.wtf(tag + "ERROR", t.message.toString())
            }
        }
    }

    //Book Room
    private val _bookRoomResult = MutableLiveData<BookRoomModal>()
    val bookRoomResult: LiveData<BookRoomModal> = _bookRoomResult
    fun bookRoom(
        bookingId: String,
        paymentID: String,
        signature: String,
        guestDetails: List<String>,
        selectedGuest: String
    ) {


        viewModelScope.launch(Dispatchers.IO) {
            _loading.postValue(true)
            try {
                val response = individualRepo.bookRoom(bookingId,paymentID,signature,guestDetails,selectedGuest)
                if (response.isSuccessful) {
//                    val res = response.body()
//                    toastMessageLiveData.postValue(res?.message)
                    _bookRoomResult.postValue(response.body())
                    Log.wtf(tag, response.body().toString())
                    _loading.postValue(false)
                } else {
                    Log.wtf(tag + "ELSE", response.message().toString())
                    toastMessageLiveData.postValue(response.message())
                    _loading.postValue(false)
                }

            } catch (t: Throwable) {
                _loading.postValue(false)
                toastMessageLiveData.postValue(t.message)
                Log.wtf(tag + "ERROR", t.message.toString())
            }
        }
    }




    fun getBookingHistory(): LiveData<PagingData<BookingHistoryData>> {
        return Pager(
            config = PagingConfig(pageSize = 20),
            pagingSourceFactory = { BookingHistoryPagingSource( individualRepo) }
        ).liveData.cachedIn(viewModelScope)
    }




    //Booking Summary
    private val _bookingSummaryResult = MutableLiveData<BookingSummaryModal>()
    val bookingSummaryResult: LiveData<BookingSummaryModal> = _bookingSummaryResult
    fun getBookingSummary(bookingId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _loading.postValue(true)
            try {
                val response = individualRepo.getBookingSummary(bookingId)
                if (response.isSuccessful) {
//                    val res = response.body()
//                    toastMessageLiveData.postValue(res?.message)
                    _bookingSummaryResult.postValue(response.body())
                    Log.wtf(tag, response.body().toString())
                    _loading.postValue(false)
                } else {
                    Log.wtf(tag + "ELSE", response.message().toString())
                    toastMessageLiveData.postValue(response.message())
                    _loading.postValue(false)
                }

            } catch (t: Throwable) {
                _loading.postValue(false)
                toastMessageLiveData.postValue(t.message)
                Log.wtf(tag + "ERROR", t.message.toString())
            }
        }
    }

    //acceptRejectTableBanquet
    private val _acceptRejectTableBanquetResult = MutableLiveData<AcceptRejectBookingModal>()
    val acceptRejectTableBanquetResult: LiveData<AcceptRejectBookingModal> = _acceptRejectTableBanquetResult
    fun acceptRejectTableBanquet(bookingId: String,status: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _loading.postValue(true)
            try {
                val response = individualRepo.acceptRejectTableBanquet(bookingId,status)

                println("asjfaklsjf.  response. $response")
                if (response.isSuccessful) {
//                    val res = response.body()
//                    toastMessageLiveData.postValue(res?.message)
                    _acceptRejectTableBanquetResult.postValue(response.body())
                    Log.wtf(tag, response.body().toString())
                    _loading.postValue(false)
                } else {
                    Log.wtf(tag + "ELSE", response.message().toString())
                    toastMessageLiveData.postValue(response.message())
                    _loading.postValue(false)
                }

            } catch (t: Throwable) {
                _loading.postValue(false)
                toastMessageLiveData.postValue(t.message)
                Log.wtf(tag + "ERROR", t.message.toString())
            }
        }
    }

    //Download Booking Invoice
    private val _downloadBookingInvoiceResult = MutableLiveData<ExportChatModal>()
    val downloadBookingInvoiceResult: LiveData<ExportChatModal> = _downloadBookingInvoiceResult
    fun downloadBookingInvoice(bookingId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _loading.postValue(true)
            try {
                val response = individualRepo.downloadBookingInvoice(bookingId)
                if (response.isSuccessful) {
//                    val res = response.body()
//                    toastMessageLiveData.postValue(res?.message)
                    _downloadBookingInvoiceResult.postValue(response.body())
                    Log.wtf(tag, response.body().toString())
                    _loading.postValue(false)
                } else {
                    Log.wtf(tag + "ELSE", response.message().toString())
                    toastMessageLiveData.postValue(response.message())
                    _loading.postValue(false)
                }

            } catch (t: Throwable) {
                _loading.postValue(false)
                toastMessageLiveData.postValue(t.message)
                Log.wtf(tag + "ERROR", t.message.toString())
            }
        }
    }

    //Cancel Booking
    private val _cancelBookingResult = MutableLiveData<DeleteModal>()
    val cancelBookingResult: LiveData<DeleteModal> = _cancelBookingResult
    fun cancelBooking(bookingId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _loading.postValue(true)
            try {
                val response = individualRepo.cancelBooking(bookingId)
                if (response.isSuccessful) {
//                    val res = response.body()
//                    toastMessageLiveData.postValue(res?.message)
                    _cancelBookingResult.postValue(response.body())
                    Log.wtf(tag, response.body().toString())
                    _loading.postValue(false)
                } else {
                    Log.wtf(tag + "ELSE", response.message().toString())
                    toastMessageLiveData.postValue(response.message())
                    _loading.postValue(false)
                }

            } catch (t: Throwable) {
                _loading.postValue(false)
                toastMessageLiveData.postValue(t.message)
                Log.wtf(tag + "ERROR", t.message.toString())
            }
        }
    }


    //Sent Otp TO Number
    private val _sentOtpToNumberResult = MutableLiveData<DeleteModal>()
    val sentOtpToNumberResult: LiveData<DeleteModal> = _sentOtpToNumberResult
    fun sentOtpToNumber(dialCode: String,phoneNumber: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _otpLoading.postValue(true)
            try {
                val response = individualRepo.sentOtpToNumber(dialCode,phoneNumber)
                if (response.isSuccessful) {
                    val res = response.body()
                    otpMessageLiveData.postValue(res?.message ?: "")
                    _sentOtpToNumberResult.postValue(response.body())
                    Log.wtf(tag, response.body().toString())
                    _otpLoading.postValue(false)
                } else {
                    Log.wtf(tag + "ELSE", response.message().toString())
                    otpMessageLiveData.postValue(response.message())
                    _otpLoading.postValue(false)
                }

            } catch (t: Throwable) {
                _otpLoading.postValue(false)
                otpMessageLiveData.postValue(t.message)
                Log.wtf(tag + "ERROR", t.message.toString())
            }
        }
    }


    //Verify OTP with Number
    private val _verifyNumberWithOtpResult = MutableLiveData<VerifyMobileNumberModal>()
    val verifyNumberWithOtpResult: LiveData<VerifyMobileNumberModal> = _verifyNumberWithOtpResult
    fun verifyNumberWithOtp(dialCode: String,phoneNumber: String,otp: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _otpLoading.postValue(true)
            try {
                val response = individualRepo.verifyNumberWithOtp(dialCode,phoneNumber,otp)
                if (response.isSuccessful) {
                    val res = response.body()
                    otpMessageLiveData.postValue(res?.message ?: "")
                    _verifyNumberWithOtpResult.postValue(response.body())
                    Log.wtf(tag, response.body().toString())
                    _otpLoading.postValue(false)
                } else {
                    Log.wtf(tag + "ELSE", response.message().toString())
                    otpMessageLiveData.postValue(response.message())
                    _otpLoading.postValue(false)
                }

            } catch (t: Throwable) {
                _otpLoading.postValue(false)
                otpMessageLiveData.postValue(t.message)
                Log.wtf(tag + "ERROR", t.message.toString())
            }
        }
    }

    //Book a Table
    private val _bookATableResult = MutableLiveData<BookTableModal>()
    val bookATableResult: LiveData<BookTableModal> = _bookATableResult
    fun bookATable(numberOfGuests: String,date: String,time: String,businessProfileID: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _loading.postValue(true)
            try {
                val response = individualRepo.bookATable(numberOfGuests,date,time,businessProfileID)
                if (response.isSuccessful) {
                    val res = response.body()
//                    toastMessageLiveData.postValue(res?.message ?: "")
                    _bookATableResult.postValue(response.body())
                    Log.wtf(tag, response.body().toString())
                    _loading.postValue(false)
                } else {
                    Log.wtf(tag + "ELSE", response.message().toString())
                    toastMessageLiveData.postValue(response.message())
                    _loading.postValue(false)
                }

            } catch (t: Throwable) {
                _loading.postValue(false)
                toastMessageLiveData.postValue(t.message)
                Log.wtf(tag + "ERROR", t.message.toString())
            }
        }
    }

    //Book a Banquet
    private val _bookABanquetResult = MutableLiveData<BookBanquetModal>()
    val bookABanquetResult: LiveData<BookBanquetModal> = _bookABanquetResult
    fun bookABanquet(checkIn: String,checkOut: String,businessProfileID: String,numberOfGuests: Int,typeOfEvent: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _loading.postValue(true)
            try {
                val response = individualRepo.bookABanquet(checkIn,checkOut,businessProfileID,numberOfGuests,typeOfEvent)
                if (response.isSuccessful) {
                    val res = response.body()
//                    toastMessageLiveData.postValue(res?.message ?: "")
                    _bookABanquetResult.postValue(response.body())
                    Log.wtf(tag, response.body().toString())
                    _loading.postValue(false)
                } else {
                    Log.wtf(tag + "ELSE", response.message().toString())
                    otpMessageLiveData.postValue(response.message())
                    _loading.postValue(false)
                }

            } catch (t: Throwable) {
                _loading.postValue(false)
                otpMessageLiveData.postValue(t.message)
                Log.wtf(tag + "ERROR", t.message.toString())
            }
        }
    }


    //Post Job
    private val _postJobResult = MutableLiveData<PostJobModal>()
    val postJobResult: LiveData<PostJobModal> = _postJobResult
    fun postJob(title: String,designation: String,description: String,jobType: String,salary: String,joiningDate: String,numberOfVacancies: String,experience: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _otpLoading.postValue(true)
            try {
                val response = individualRepo.postJob(title,designation,description,jobType,salary,joiningDate,numberOfVacancies,experience)
                if (response.isSuccessful) {
                    val res = response.body()
                    otpMessageLiveData.postValue(res?.message ?: "")
                    _postJobResult.postValue(response.body())
                    Log.wtf(tag, response.body().toString())
                    _otpLoading.postValue(false)
                } else {
                    Log.wtf(tag + "ELSE", response.message().toString())
                    otpMessageLiveData.postValue(response.message())
                    _otpLoading.postValue(false)
                }

            } catch (t: Throwable) {
                _otpLoading.postValue(false)
                otpMessageLiveData.postValue(t.message)
                Log.wtf(tag + "ERROR", t.message.toString())
            }
        }
    }

    //Job Details
    private val _jobDetailsResult = MutableLiveData<UIState<JobDetailsModal>>()
    val jobDetailsResult: LiveData<UIState<JobDetailsModal>> = _jobDetailsResult

    fun getJobDetails(jobId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _jobDetailsResult.postValue(UIState.loading())

            try {
                val response = individualRepo.getJobDetails(jobId)

                if (response.isSuccessful) {
                    val data = response.body()
                    _jobDetailsResult.postValue(
                        UIState.success(data, data?.message ?: "Success")
                    )
                    Log.d(tag, "Job details success: $data")
                } else {
                    val message = response.message()
                    _jobDetailsResult.postValue(UIState.error(null, message))
                    Log.e(tag, "Error response: $message")
                }

            } catch (e: Exception) {
                val errorMessage = e.message ?: "Unexpected error"
                _jobDetailsResult.postValue(UIState.error(null, errorMessage))
                Log.e(tag, "Exception: $errorMessage")
            }
        }
    }


}