package com.thehotelmedia.android.modals.notificationStatus

import com.google.gson.annotations.SerializedName


data class Data (

  @SerializedName("notifications" ) var notifications : Notifications? = Notifications(),
  @SerializedName("messages"      ) var messages      : Messages?      = Messages()

)