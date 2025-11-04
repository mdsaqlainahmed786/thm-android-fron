package com.thehotelmedia.android.modals.notificationStatus

import com.google.gson.annotations.SerializedName


data class Notifications (

  @SerializedName("hasUnreadMessages" ) var hasUnreadMessages : Boolean? = null,
  @SerializedName("count"             ) var count             : Int?     = null

)