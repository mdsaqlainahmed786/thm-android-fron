package com.thehotelmedia.android.modals.booking.checkIn

import com.google.gson.annotations.SerializedName


data class Booking (

  @SerializedName("status"            ) var status            : String? = null,
  @SerializedName("adults"            ) var adults            : Int?    = null,
  @SerializedName("children"          ) var children          : Int?    = null,
  @SerializedName("childrenAge"         ) var childrenAge         : ArrayList<Int>          = arrayListOf(),
  @SerializedName("_id"               ) var Id                : String? = null,
  @SerializedName("checkIn"           ) var checkIn           : String? = null,
  @SerializedName("checkOut"          ) var checkOut          : String? = null,
  @SerializedName("bookingID"         ) var bookingID         : String? = null,
  @SerializedName("userID"            ) var userID            : String? = null,
  @SerializedName("businessProfileID" ) var businessProfileID : String? = null,
  @SerializedName("createdAt"         ) var createdAt         : String? = null,
  @SerializedName("updatedAt"         ) var updatedAt         : String? = null,
  @SerializedName("__v"               ) var _v                : Int?    = null

)