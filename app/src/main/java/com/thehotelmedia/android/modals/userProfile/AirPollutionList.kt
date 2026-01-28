package com.thehotelmedia.android.modals.userProfile

import com.google.gson.annotations.SerializedName
import com.thehotelmedia.android.modals.weatherOrAqi.aqi.Main as AqiMain
import com.thehotelmedia.android.modals.weatherOrAqi.aqi.Components

data class AirPollutionList(
    // Air pollution's "main" contains AQI index, not temperature fields.
    @SerializedName("main"       ) var main       : AqiMain?       = AqiMain(),
    @SerializedName("components" ) var components : Components? = Components(),
    @SerializedName("dt"         ) var dt         : Int?        = null
)
