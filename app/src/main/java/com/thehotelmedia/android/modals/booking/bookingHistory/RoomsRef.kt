package com.thehotelmedia.android.modals.booking.bookingHistory

import com.google.gson.annotations.SerializedName


data class RoomsRef (

  @SerializedName("_id"      ) var Id       : String? = null,
  @SerializedName("bedType"  ) var bedType  : String? = null,
  @SerializedName("roomType" ) var roomType : String? = null,
  @SerializedName("title"    ) var title    : String? = null

)