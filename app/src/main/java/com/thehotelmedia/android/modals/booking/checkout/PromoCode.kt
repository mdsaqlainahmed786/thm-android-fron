package com.thehotelmedia.android.modals.booking.checkout

import com.google.gson.annotations.SerializedName


data class PromoCode (

    @SerializedName("name"        ) var name        : String? = null,
    @SerializedName("description" ) var description : String? = null

)
