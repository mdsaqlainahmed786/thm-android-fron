package com.thehotelmedia.android.modals.profileData.profile

import com.google.gson.annotations.SerializedName

data class AmenitiesRef (

    @SerializedName("_id"   ) var Id    : String? = null,
    @SerializedName("icon"  ) var icon  : String? = null,
    @SerializedName("name"  ) var name  : String? = null,
    @SerializedName("order" ) var order : Int?    = null,
    @SerializedName("minMaxTemp" ) var minMaxTemp : String?    = null,
    @SerializedName("aqi" ) var aqi : Int?    = null,


)
