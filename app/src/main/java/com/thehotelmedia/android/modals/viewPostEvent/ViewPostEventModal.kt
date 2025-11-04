package com.thehotelmedia.android.modals.viewPostEvent

import com.google.gson.annotations.SerializedName
import com.thehotelmedia.android.modals.viewPostEvent.Data


data class ViewPostEventModal (

  @SerializedName("status"     ) var status     : Boolean?        = null,
  @SerializedName("statusCode" ) var statusCode : Int?            = null,
  @SerializedName("message"    ) var message    : String?         = null,
  @SerializedName("data"       ) var data       : Data?           = Data()

)