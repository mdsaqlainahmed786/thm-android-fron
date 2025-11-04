package com.thehotelmedia.android.modals.notificationStatus

import com.google.gson.annotations.SerializedName


data class Messages (

  @SerializedName("hasUnreadMessages" ) var hasUnreadMessages : Boolean? = null,
  @SerializedName("count"             ) var count             : Int?     = null

)