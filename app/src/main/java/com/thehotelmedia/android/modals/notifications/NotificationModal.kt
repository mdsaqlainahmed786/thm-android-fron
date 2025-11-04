package com.thehotelmedia.android.modals.notifications

import com.google.gson.annotations.SerializedName


data class NotificationModal (

  @SerializedName("status"         ) var status         : Boolean?        = null,
  @SerializedName("statusCode"     ) var statusCode     : Int?            = null,
  @SerializedName("message"        ) var message        : String?         = null,
  @SerializedName("data"           ) var notificationData           : ArrayList<NotificationData> = arrayListOf(),
  @SerializedName("pageNo"         ) var pageNo         : Int?            = null,
  @SerializedName("totalPages"     ) var totalPages     : Int?            = null,
  @SerializedName("totalResources" ) var totalResources : Int?            = null

)