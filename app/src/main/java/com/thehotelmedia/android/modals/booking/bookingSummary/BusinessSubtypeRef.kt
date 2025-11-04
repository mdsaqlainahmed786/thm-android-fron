package com.thehotelmedia.android.modals.booking.bookingSummary

import com.google.gson.annotations.SerializedName


data class BusinessSubtypeRef (

  @SerializedName("_id"  ) var Id   : String? = null,
  @SerializedName("name" ) var name : String? = null

)