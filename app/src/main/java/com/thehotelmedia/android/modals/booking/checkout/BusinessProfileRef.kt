package com.thehotelmedia.android.modals.booking.checkout

import com.google.gson.annotations.SerializedName


data class BusinessProfileRef (

    @SerializedName("_id"                ) var Id                 : String?             = null,
    @SerializedName("rating"             ) var rating             : Double?                = null,
    @SerializedName("profilePic"         ) var profilePic         : ProfilePic?         = ProfilePic(),
    @SerializedName("name"               ) var name               : String?             = null,
    @SerializedName("address"            ) var address            : Address?            = Address(),
    @SerializedName("businessTypeRef"    ) var businessTypeRef    : BusinessTypeRef?    = BusinessTypeRef(),
    @SerializedName("businessSubtypeRef" ) var businessSubtypeRef : BusinessSubtypeRef? = BusinessSubtypeRef(),
    @SerializedName("userID"             ) var userID             : String?             = null

)
