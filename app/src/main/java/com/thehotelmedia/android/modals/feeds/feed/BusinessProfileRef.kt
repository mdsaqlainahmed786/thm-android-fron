package com.thehotelmedia.android.modals.feeds.feed

import com.google.gson.annotations.SerializedName
import com.thehotelmedia.android.modals.profileData.profile.BusinessSubtypeRef


data class BusinessProfileRef (

    @SerializedName("_id"                ) var Id                    : String?             = null,
    @SerializedName("profilePic"         ) var profilePic            : ProfilePic?         = ProfilePic(),
    @SerializedName("name"               ) var name                  : String?             = null,
    @SerializedName("rating"             ) var businessRating        : Double?             = null,
    @SerializedName("address"            ) var address               : Address?            = Address(),
    @SerializedName("businessTypeRef"    ) var businessTypeRef       : BusinessTypeRef?    = BusinessTypeRef(),
    @SerializedName("businessSubtypeRef" ) var businessSubtypeRef    : BusinessSubtypeRef? = BusinessSubtypeRef()

)