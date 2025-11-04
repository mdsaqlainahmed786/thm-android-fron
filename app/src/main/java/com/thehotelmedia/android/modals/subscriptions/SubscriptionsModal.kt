package com.thehotelmedia.android.modals.subscriptions

import com.google.gson.annotations.SerializedName
import com.thehotelmedia.android.modals.subscriptions.Data


data class SubscriptionsModal (

  @SerializedName("status"     ) var status     : Boolean? = null,
  @SerializedName("statusCode" ) var statusCode : Int?     = null,
  @SerializedName("message"    ) var message    : String?  = null,
  @SerializedName("data"       ) var data       : Data?    = Data()

)