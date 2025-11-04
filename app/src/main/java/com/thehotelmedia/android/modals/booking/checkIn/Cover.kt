package com.thehotelmedia.android.modals.booking.checkIn

import com.google.gson.annotations.SerializedName


data class Cover (

  @SerializedName("_id"          ) var Id           : String?  = null,
  @SerializedName("isCoverImage" ) var isCoverImage : Boolean? = null,
  @SerializedName("sourceUrl"    ) var sourceUrl    : String?  = null,
  @SerializedName("thumbnailUrl" ) var thumbnailUrl : String?  = null

)