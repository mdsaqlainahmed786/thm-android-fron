package com.thehotelmedia.android.modals.booking.bookTable

import com.google.gson.annotations.SerializedName


data class BookTableModal (

  @SerializedName("status"     ) var status     : Boolean? = null,
  @SerializedName("statusCode" ) var statusCode : Int?     = null,
  @SerializedName("message"    ) var message    : String?  = null,
  @SerializedName("data"       ) var data       : Data?    = Data()

)