package com.thehotelmedia.android.modals.weatherOrAqi.aqi

import com.google.gson.annotations.SerializedName


data class Main (

  @SerializedName("aqi" ) var aqi : Int? = null

)