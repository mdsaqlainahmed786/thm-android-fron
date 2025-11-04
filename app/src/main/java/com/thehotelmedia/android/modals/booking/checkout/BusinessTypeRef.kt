package com.thehotelmedia.android.modals.booking.checkout

import com.google.gson.annotations.SerializedName


data class BusinessTypeRef (

    @SerializedName("_id"  ) var Id   : String? = null,
    @SerializedName("icon" ) var icon : String? = null,
    @SerializedName("name" ) var name : String? = null

)
