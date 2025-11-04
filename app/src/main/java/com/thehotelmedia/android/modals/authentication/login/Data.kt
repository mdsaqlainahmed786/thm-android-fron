package com.thehotelmedia.android.modals.authentication.login

import com.google.gson.annotations.SerializedName


data class Data (

  @SerializedName("bio"                ) var bio                : String?     = null,
  @SerializedName("accountType"        ) var accountType        : String?     = null,
  @SerializedName("isVerified"         ) var isVerified         : Boolean?    = null,
  @SerializedName("isApproved"         ) var isApproved         : Boolean?    = null,
  @SerializedName("isActivated"        ) var isActivated        : Boolean?    = null,
  @SerializedName("isDeleted"          ) var isDeleted          : Boolean?    = null,
  @SerializedName("hasProfilePicture"  ) var hasProfilePicture  : Boolean?    = null,
  @SerializedName("acceptedTerms"      ) var acceptedTerms      : Boolean?    = null,
  @SerializedName("profession"         ) var profession         : String?     = null,
  @SerializedName("username"           ) var username           : String?     = null,
  @SerializedName("email"              ) var email              : String?     = null,
  @SerializedName("name"               ) var name               : String?     = null,
  @SerializedName("dialCode"           ) var dialCode           : String?     = null,
  @SerializedName("phoneNumber"        ) var phoneNumber        : String?     = null,
  @SerializedName("profilePic"         ) var profilePic         : ProfilePic? = ProfilePic(),
  @SerializedName("createdAt"          ) var createdAt          : String?     = null,
  @SerializedName("updatedAt"          ) var updatedAt          : String?     = null,
  @SerializedName("__v"                ) var _v                 : Int?        = null,
  @SerializedName("id"                 ) var id                 : String?     = null,
  @SerializedName("isDocumentUploaded" ) var isDocumentUploaded : Boolean?    = null,
  @SerializedName("businessProfileRef" ) var businessProfile    : BusinessProfile? = BusinessProfile(),
  @SerializedName("hasAmenities"       ) var hasAmenities       : Boolean?    = null,
  @SerializedName("hasSubscription"    ) var hasSubscription    : Boolean?    = null,
  @SerializedName("accessToken"        ) var accessToken        : String?     = null,
  @SerializedName("refreshToken"       ) var refreshToken       : String?     = null

)