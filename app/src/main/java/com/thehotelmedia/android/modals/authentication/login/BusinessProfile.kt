package com.thehotelmedia.android.modals.authentication.login

import com.google.gson.annotations.SerializedName


data class BusinessProfile (

  @SerializedName("website"           ) var website           : String?           = null,
  @SerializedName("gstn"              ) var gstn              : String?           = null,
  @SerializedName("amenities"         ) var amenities         : ArrayList<String> = arrayListOf(),
  @SerializedName("businessTypeID"    ) var businessTypeID    : String?           = null,
  @SerializedName("businessSubTypeID" ) var businessSubTypeID : String?           = null,
  @SerializedName("name"              ) var name              : String?           = null,
  @SerializedName("bio"               ) var bio               : String?           = null,
  @SerializedName("address"           ) var address           : Address?          = Address(),
  @SerializedName("email"             ) var email             : String?           = null,
  @SerializedName("phoneNumber"       ) var phoneNumber       : String?           = null,
  @SerializedName("dialCode"          ) var dialCode          : String?           = null,
  @SerializedName("profilePic"        ) var profilePic        : ProfilePic?       = ProfilePic(),
  @SerializedName("createdAt"         ) var createdAt         : String?           = null,
  @SerializedName("updatedAt"         ) var updatedAt         : String?           = null,
  @SerializedName("__v"               ) var _v                : Int?              = null,
  @SerializedName("id"                ) var id                : String?           = null

)