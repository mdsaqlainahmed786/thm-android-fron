package com.thehotelmedia.android.modals.checkIn

import com.google.gson.annotations.SerializedName


data class OpeningHours (

  @SerializedName("open_now" ) var openNow : Boolean? = null

)