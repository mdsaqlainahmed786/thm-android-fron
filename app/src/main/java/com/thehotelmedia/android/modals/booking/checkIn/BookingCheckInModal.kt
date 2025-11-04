package com.thehotelmedia.android.modals.booking.checkIn

import com.google.gson.annotations.SerializedName


data class BookingCheckInModal (

  @SerializedName("status"     ) var status     : Boolean? = null,
  @SerializedName("statusCode" ) var statusCode : Int?     = null,
  @SerializedName("message"    ) var message    : String?  = null,
  @SerializedName("data"       ) var data       : Data?    = Data()

)