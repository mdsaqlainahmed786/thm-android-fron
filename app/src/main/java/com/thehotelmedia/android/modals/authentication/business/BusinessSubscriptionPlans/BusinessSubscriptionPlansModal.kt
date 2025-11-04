package com.thehotelmedia.android.modals.authentication.business.BusinessSubscriptionPlans

import com.google.gson.annotations.SerializedName


data class BusinessSubscriptionPlansModal (

  @SerializedName("status"     ) var status     : Boolean?        = null,
  @SerializedName("statusCode" ) var statusCode : Int?            = null,
  @SerializedName("message"    ) var message    : String?         = null,
  @SerializedName("data"       ) var subscriptionData       : ArrayList<SubscriptionData> = arrayListOf()

)