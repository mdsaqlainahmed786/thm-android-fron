package com.thehotelmedia.android.modals.booking.bookingHistory

import com.google.gson.annotations.SerializedName


data class BookedRoom (

  @SerializedName("roomID"   ) var roomID   : String? = null,
  @SerializedName("price"    ) var price    : Int?    = null,
  @SerializedName("quantity" ) var quantity : Int?    = null,
  @SerializedName("nights"   ) var nights   : Int?    = null

)