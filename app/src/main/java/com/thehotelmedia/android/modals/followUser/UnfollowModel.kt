package com.thehotelmedia.android.modals.followUser

import com.google.gson.annotations.SerializedName


data class UnfollowModel (

  @SerializedName("status"     ) var status     : Boolean? = null,
  @SerializedName("statusCode" ) var statusCode : Int?     = null,
  @SerializedName("message"    ) var message    : String?  = null,
  @SerializedName("data"       ) var data       : String?  = null

)