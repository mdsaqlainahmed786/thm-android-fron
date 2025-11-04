package com.thehotelmedia.android.modals.booking.bookingHistory

import com.google.gson.annotations.SerializedName


data class BookingHistoryModal (

  @SerializedName("status"         ) var status         : Boolean?        = null,
  @SerializedName("statusCode"     ) var statusCode     : Int?            = null,
  @SerializedName("message"        ) var message        : String?         = null,
  @SerializedName("data"           ) var bookingHistoryData: ArrayList<BookingHistoryData> = arrayListOf(),
  @SerializedName("pageNo"         ) var pageNo         : Int?            = null,
  @SerializedName("totalPages"     ) var totalPages     : Int?            = null,
  @SerializedName("totalResources" ) var totalResources : Int?            = null

)