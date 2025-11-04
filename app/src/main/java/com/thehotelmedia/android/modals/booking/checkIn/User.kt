package com.thehotelmedia.android.modals.booking.checkIn

import com.google.gson.annotations.SerializedName


data class User (

    @SerializedName("email"          ) var email          : String?  = null,
    @SerializedName("name"           ) var name           : String?  = null,
    @SerializedName("dialCode"       ) var dialCode       : String?  = null,
    @SerializedName("phoneNumber"    ) var phoneNumber    : String?  = null,
    @SerializedName("mobileVerified" ) var mobileVerified : Boolean? = null

)
