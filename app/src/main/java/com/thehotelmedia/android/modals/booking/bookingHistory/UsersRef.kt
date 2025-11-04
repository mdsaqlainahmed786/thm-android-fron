package com.thehotelmedia.android.modals.booking.bookingHistory

import com.google.gson.annotations.SerializedName
import com.thehotelmedia.android.modals.booking.bookingHistory.ProfilePic


data class UsersRef (

    @SerializedName("_id"         ) var Id          : String?     = null,
    @SerializedName("accountType" ) var accountType : String?     = null,
    @SerializedName("profilePic"  ) var profilePic  : ProfilePic? = ProfilePic(),
    @SerializedName("username"    ) var username    : String?     = null,
    @SerializedName("name"        ) var name        : String?     = null

)