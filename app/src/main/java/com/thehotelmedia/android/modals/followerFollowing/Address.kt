package com.thehotelmedia.android.modals.followerFollowing

import com.google.gson.annotations.SerializedName


data class Address (

    @SerializedName("street"        ) var street        : String?        = null,
    @SerializedName("city"          ) var city          : String?        = null,
    @SerializedName("state"         ) var state         : String?        = null,
    @SerializedName("zipCode"       ) var zipCode       : String?        = null,
    @SerializedName("country"       ) var country       : String?        = null,
    @SerializedName("lat"           ) var lat           : Double?        = null,
    @SerializedName("lng"           ) var lng           : Double?        = null

)
