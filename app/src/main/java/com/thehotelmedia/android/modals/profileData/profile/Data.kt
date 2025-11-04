package com.thehotelmedia.android.modals.profileData.profile

import com.google.gson.annotations.SerializedName
import com.thehotelmedia.android.modals.userProfile.Weather


data class Data (

  @SerializedName("posts"             ) var posts             : Int?        = null,
  @SerializedName("follower"          ) var follower          : Int?        = null,
  @SerializedName("following"         ) var following         : Int?        = null,
  @SerializedName("profileCompleted"  ) var profileCompleted  : Double?     = null,
  @SerializedName("_id"               ) var Id                : String?     = null,
  @SerializedName("bio"               ) var bio               : String?     = null,
  @SerializedName("accountType"       ) var accountType       : String?     = null,
  @SerializedName("isVerified"        ) var isVerified        : Boolean?    = null,
  @SerializedName("isApproved"        ) var isApproved        : Boolean?    = null,
  @SerializedName("isActivated"       ) var isActivated       : Boolean?    = null,
  @SerializedName("isDeleted"         ) var isDeleted         : Boolean?    = null,
  @SerializedName("hasProfilePicture" ) var hasProfilePicture : Boolean?    = null,
  @SerializedName("username"          ) var username          : String?     = null,
  @SerializedName("email"             ) var email             : String?     = null,
  @SerializedName("name"              ) var name              : String?     = null,
  @SerializedName("dialCode"          ) var dialCode          : String?     = null,
  @SerializedName("phoneNumber"       ) var phoneNumber       : String?     = null,
  @SerializedName("profilePic"        ) var profilePic        : ProfilePic? = ProfilePic(),
  @SerializedName("acceptedTerms"     ) var acceptedTerms     : Boolean?    = null,
  @SerializedName("businessProfileID"  ) var businessProfileID  : String?             = null,
  @SerializedName("businessProfileRef" ) var businessProfileRef : BusinessProfileRef? = BusinessProfileRef(),
  @SerializedName("address"             ) var address             : Address?    = Address(),
  @SerializedName("weather"             ) var weather             : Weather?            = Weather(),
  @SerializedName("privateAccount"       ) var privateAccount       : Boolean?    = null,
  @SerializedName("notificationEnabled"  ) var notificationEnabled  : Boolean?    = null,



)