package com.thehotelmedia.android.modals.subscriptions

import com.google.gson.annotations.SerializedName


data class Subscription (

  @SerializedName("_id"                ) var id                 : String? = null,
  @SerializedName("businessProfileID"  ) var businessProfileID  : String? = null,
  @SerializedName("userID"             ) var userID             : String? = null,
  @SerializedName("subscriptionPlanID" ) var subscriptionPlanID : String? = null,
  @SerializedName("expirationDate"     ) var expirationDate     : String? = null,
  @SerializedName("createdAt"          ) var createdAt          : String? = null,
  @SerializedName("updatedAt"          ) var updatedAt          : String? = null,
  @SerializedName("name"               ) var name               : String? = null,
  @SerializedName("image"              ) var image              : String? = null,
  @SerializedName("remainingDays"      ) var remainingDays      : Int?    = null

)