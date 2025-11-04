package com.thehotelmedia.android.modals.transactions

import com.google.gson.annotations.SerializedName


data class SubscriptionPlanRef (

  @SerializedName("_id"   ) var Id    : String? = null,
  @SerializedName("name"  ) var name  : String? = null,
  @SerializedName("image" ) var image : String? = null

)