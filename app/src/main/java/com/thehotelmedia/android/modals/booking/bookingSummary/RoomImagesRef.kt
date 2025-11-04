package com.thehotelmedia.android.modals.booking.bookingSummary

import com.google.gson.annotations.SerializedName


data class RoomImagesRef (

  @SerializedName("_id"          ) var Id           : String?  = null,
  @SerializedName("isCoverImage" ) var isCoverImage : Boolean? = null,
  @SerializedName("sourceUrl"    ) var sourceUrl    : String?  = null,
  @SerializedName("thumbnailUrl" ) var thumbnailUrl : String?  = null

)