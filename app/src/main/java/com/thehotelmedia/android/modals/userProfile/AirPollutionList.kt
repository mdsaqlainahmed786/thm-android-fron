package com.thehotelmedia.android.modals.userProfile

import com.google.gson.annotations.SerializedName
import com.thehotelmedia.android.modals.weatherOrAqi.aqi.Components

data class AirPollutionList(
    @SerializedName("main"       ) var main       : Main?       = Main(),
    @SerializedName("components" ) var components : Components? = Components(),
    @SerializedName("dt"         ) var dt         : Int?        = null
)
