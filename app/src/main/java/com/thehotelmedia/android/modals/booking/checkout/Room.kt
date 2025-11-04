package com.thehotelmedia.android.modals.booking.checkout

import com.google.gson.annotations.SerializedName


data class Room (

  @SerializedName("title"   ) var title   : String? = null,
  @SerializedName("bedType" ) var bedType : String? = null
)