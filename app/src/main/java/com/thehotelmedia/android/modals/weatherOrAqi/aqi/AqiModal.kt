package com.thehotelmedia.android.modals.weatherOrAqi.aqi

import com.google.gson.annotations.SerializedName


data class AqiModal (

  @SerializedName("coord" ) var coord : Coord?          = Coord(),
  @SerializedName("list"  ) var list  : ArrayList<List> = arrayListOf()

)