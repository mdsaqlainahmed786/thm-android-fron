package com.thehotelmedia.android.modals.authentication.business.businessSignUp

import com.google.gson.annotations.SerializedName


data class Data (

  @SerializedName("bio"               ) var bio               : String?  = null,
  @SerializedName("accountType"       ) var accountType       : String?  = null,
  @SerializedName("isVerified"        ) var isVerified        : Boolean? = null,
  @SerializedName("isApproved"        ) var isApproved        : Boolean? = null,
  @SerializedName("isActivated"       ) var isActivated       : Boolean? = null,
  @SerializedName("isDeleted"         ) var isDeleted         : Boolean? = null,
  @SerializedName("hasProfilePicture" ) var hasProfilePicture : Boolean? = null,
  @SerializedName("acceptedTerms"     ) var acceptedTerms     : Boolean? = null,
  @SerializedName("email"             ) var email             : String?  = null,
  @SerializedName("fullName"          ) var fullName          : String?  = null,
  @SerializedName("dialCode"          ) var dialCode          : String?  = null,
  @SerializedName("phoneNumber"       ) var phoneNumber       : String?  = null,
  @SerializedName("businessProfileID" ) var businessProfileID : String?  = null,
  @SerializedName("createdAt"         ) var createdAt         : String?  = null,
  @SerializedName("updatedAt"         ) var updatedAt         : String?  = null,
  @SerializedName("__v"               ) var _v                : Int?     = null,
  @SerializedName("id"                ) var id                : String?  = null

)