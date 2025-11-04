package com.thehotelmedia.android.modals.booking.bookingSummary

import com.google.gson.annotations.SerializedName


data class GeoCoordinate (

  @SerializedName("type"        ) var type        : String?           = null,
  @SerializedName("coordinates" ) var coordinates : ArrayList<Double> = arrayListOf()

)