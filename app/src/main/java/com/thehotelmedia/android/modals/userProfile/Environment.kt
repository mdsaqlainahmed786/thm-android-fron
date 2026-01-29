package com.thehotelmedia.android.modals.userProfile

import com.google.gson.annotations.SerializedName

data class Environment (
    @SerializedName("lat"       ) var lat       : Double? = null,
    @SerializedName("lng"       ) var lng       : Double? = null,
    @SerializedName("aqiIndex"  ) var aqiIndex  : Int?    = null,
    @SerializedName("aqiLabel"  ) var aqiLabel  : String? = null,
    @SerializedName("tempK"     ) var tempK     : Double? = null,
    @SerializedName("tempMinK"  ) var tempMinK  : Double? = null,
    @SerializedName("tempMaxK"  ) var tempMaxK  : Double? = null,
    @SerializedName("tempC"     ) var tempC     : Double? = null,
    @SerializedName("tempMinC"  ) var tempMinC  : Double? = null,
    @SerializedName("tempMaxC"  ) var tempMaxC  : Double? = null
)






