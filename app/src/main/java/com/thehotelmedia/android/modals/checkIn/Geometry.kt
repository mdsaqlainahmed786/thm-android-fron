package com.thehotelmedia.android.modals.checkIn

import com.google.gson.annotations.SerializedName


data class Geometry (

  @SerializedName("location" ) var location : Location? = Location(),
  @SerializedName("viewport" ) var viewport : Viewport? = Viewport()

)