package com.thehotelmedia.android.modals.weatherOrAqi.aqi

import com.google.gson.annotations.SerializedName


data class List (

    @SerializedName("main"       ) var main       : Main?       = Main(),
    @SerializedName("components" ) var components : Components? = Components(),
    @SerializedName("dt"         ) var dt         : Int?        = null

)