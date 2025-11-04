package com.thehotelmedia.android.modals.updateAddress

import com.google.gson.annotations.SerializedName


data class GeoCoordinate (

  @SerializedName("type"        ) var type        : String?           = null,
  @SerializedName("coordinates" ) var coordinates : ArrayList<Double> = arrayListOf()

)