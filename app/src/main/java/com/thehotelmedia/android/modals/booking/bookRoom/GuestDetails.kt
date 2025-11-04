package com.thehotelmedia.android.modals.booking.bookRoom

import com.google.gson.annotations.SerializedName

data class GuestDetails (

    @SerializedName("title"        ) var title        : String? = null,
    @SerializedName("fullName"     ) var fullName     : String? = null,
    @SerializedName("email"        ) var email        : String? = null,
    @SerializedName("mobileNumber" ) var mobileNumber : String? = null

)
