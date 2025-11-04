package com.thehotelmedia.android.modals.storiesActions

import com.google.gson.annotations.SerializedName


data class DeleteStoryModal (

  @SerializedName("status"     ) var status     : Boolean? = null,
  @SerializedName("statusCode" ) var statusCode : Int?     = null,
  @SerializedName("message"    ) var message    : String?  = null,
  @SerializedName("data"       ) var data       : String?  = null

)