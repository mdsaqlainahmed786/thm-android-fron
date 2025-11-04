package com.thehotelmedia.android.modals.authentication.individual.signUp

import com.google.gson.annotations.SerializedName


data class Data (

  @SerializedName("bio"               ) var bio               : String?     = null,
  @SerializedName("accountType"       ) var accountType       : String?     = null,
  @SerializedName("isVerified"        ) var isVerified        : Boolean?    = null,
  @SerializedName("isApproved"        ) var isApproved        : Boolean?    = null,
  @SerializedName("isActivated"       ) var isActivated       : Boolean?    = null,
  @SerializedName("isDeleted"         ) var isDeleted         : Boolean?    = null,
  @SerializedName("hasProfilePicture" ) var hasProfilePicture : Boolean?    = null,
  @SerializedName("email"             ) var email             : String?     = null,
  @SerializedName("name"              ) var fullName          : String?     = null,
  @SerializedName("dialCode"          ) var dialCode          : String?     = null,
  @SerializedName("phoneNumber"       ) var phoneNumber       : String?     = null,
  @SerializedName("profilePic"        ) var profilePic        : ProfilePic? = ProfilePic(),
  @SerializedName("createdAt"         ) var createdAt         : String?     = null,
  @SerializedName("updatedAt"         ) var updatedAt         : String?     = null,
  @SerializedName("__v"               ) var _v                : Int?        = null,
  @SerializedName("id"                ) var id                : String?     = null,

)