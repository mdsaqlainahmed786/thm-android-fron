package com.thehotelmedia.android.modals.insight

import com.google.gson.annotations.SerializedName


data class Location (

  @SerializedName("lat"       ) var lat       : Double? = null,
  @SerializedName("lng"       ) var lng       : Double? = null,
  @SerializedName("placeName" ) var placeName : String? = null

)