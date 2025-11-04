package com.thehotelmedia.android.modals.booking.roomDetails

import com.google.gson.annotations.SerializedName

data class LanguageSpoken (

    @SerializedName("name"      ) var name      : String? = null,
    @SerializedName("flag"      ) var flag      : String? = null,
    @SerializedName("_id"       ) var Id        : String? = null,
    @SerializedName("createdAt" ) var createdAt : String? = null,
    @SerializedName("updatedAt" ) var updatedAt : String? = null

)