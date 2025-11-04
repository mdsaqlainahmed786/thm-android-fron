package com.thehotelmedia.android.modals.checkIn

import com.google.gson.annotations.SerializedName


data class Photos (

  @SerializedName("height"            ) var height           : Int?              = null,
  @SerializedName("html_attributions" ) var htmlAttributions : ArrayList<String> = arrayListOf(),
  @SerializedName("photo_reference"   ) var photoReference   : String?           = null,
  @SerializedName("width"             ) var width            : Int?              = null

)