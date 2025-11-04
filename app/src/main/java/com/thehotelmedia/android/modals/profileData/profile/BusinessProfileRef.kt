package com.thehotelmedia.android.modals.profileData.profile

import com.google.gson.annotations.SerializedName

data class BusinessProfileRef (

    @SerializedName("_id"               ) var Id                : String?                 = null,
    @SerializedName("bio"               ) var bio               : String?                 = null,
    @SerializedName("website"           ) var website           : String?                 = null,
    @SerializedName("gstn"              ) var gstn              : String?                 = null,
    @SerializedName("amenities"         ) var amenities         : ArrayList<String>       = arrayListOf(),
    @SerializedName("profilePic"        ) var profilePic        : ProfilePic?             = ProfilePic(),
    @SerializedName("username"          ) var username          : String?                 = null,
    @SerializedName("businessTypeID"    ) var businessTypeID    : String?                 = null,
    @SerializedName("businessSubTypeID" ) var businessSubTypeID : String?                 = null,
    @SerializedName("name"              ) var name              : String?                 = null,
    @SerializedName("description"       ) var description       : String?                 = null,
    @SerializedName("address"           ) var address           : Address?                = Address(),
    @SerializedName("email"             ) var email             : String?                 = null,
    @SerializedName("rating"             ) var rating             : Double?                = null,
    @SerializedName("phoneNumber"       ) var phoneNumber       : String?                 = null,
    @SerializedName("dialCode"          ) var dialCode          : String?                 = null,
    @SerializedName("placeID"           ) var placeID           : String?                 = null,
    @SerializedName("amenitiesRef"      ) var amenitiesRef      : ArrayList<AmenitiesRef> = arrayListOf(),
    @SerializedName("businessTypeRef"    ) var businessTypeRef    : BusinessTypeRef?        = BusinessTypeRef(),
    @SerializedName("businessSubtypeRef" ) var businessSubtypeRef : BusinessSubtypeRef?     = BusinessSubtypeRef(),
    @SerializedName("businessAnswerRef"  ) var businessAnswerRef  : ArrayList<BusinessAnswerRef> = arrayListOf()

)
