package com.thehotelmedia.android.modals.booking.acceptReject

import com.google.gson.annotations.SerializedName


data class AcceptRejectBookingModal (

  @SerializedName("status"     ) var status     : Boolean? = null,
  @SerializedName("statusCode" ) var statusCode : Int?     = null,
  @SerializedName("message"    ) var message    : String?  = null,
  @SerializedName("data"       ) var data       : Data?    = Data()

)