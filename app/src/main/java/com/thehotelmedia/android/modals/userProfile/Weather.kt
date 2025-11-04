package com.thehotelmedia.android.modals.userProfile

import com.google.gson.annotations.SerializedName

data class Weather (

    @SerializedName("base"         ) var base         : String?            = null,
    @SerializedName("main"         ) var main         : Main?              = Main(),
    @SerializedName("visibility"   ) var visibility   : Int?               = null,
    @SerializedName("dt"           ) var dt           : Int?               = null,
    @SerializedName("timezone"     ) var timezone     : Int?               = null,
    @SerializedName("id"           ) var id           : Int?               = null,
    @SerializedName("name"         ) var name         : String?            = null,
    @SerializedName("cod"          ) var cod          : Int?               = null,
    @SerializedName("airPollution" ) var airPollution : AirPollution?      = AirPollution()

)
