package com.thehotelmedia.android.modals.editProfile

import com.google.gson.annotations.SerializedName


data class BusinessProfileRef (

    @SerializedName("bio"               ) var bio               : String?           = null,
    @SerializedName("website"           ) var website           : String?           = null,
    @SerializedName("gstn"              ) var gstn              : String?           = null,
    @SerializedName("amenities"         ) var amenities         : ArrayList<String> = arrayListOf(),
    @SerializedName("profilePic"        ) var profilePic        : ProfilePic?       = ProfilePic(),
    @SerializedName("username"          ) var username          : String?           = null,
    @SerializedName("businessTypeID"    ) var businessTypeID    : String?           = null,
    @SerializedName("businessSubTypeID" ) var businessSubTypeID : String?           = null,
    @SerializedName("name"              ) var name              : String?           = null,
    @SerializedName("description"       ) var description       : String?           = null,
    @SerializedName("address"           ) var address           : Address?          = Address(),
    @SerializedName("email"             ) var email             : String?           = null,
    @SerializedName("phoneNumber"       ) var phoneNumber       : String?           = null,
    @SerializedName("dialCode"          ) var dialCode          : String?           = null,
    @SerializedName("placeID"           ) var placeID           : String?           = null,
    @SerializedName("createdAt"         ) var createdAt         : String?           = null,
    @SerializedName("updatedAt"         ) var updatedAt         : String?           = null,
    @SerializedName("__v"               ) var _v                : Int?              = null,
    @SerializedName("id"                ) var id                : String?           = null

)