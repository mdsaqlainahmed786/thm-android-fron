package com.thehotelmedia.android.modals.authentication.business.subscriptionCheckOut

import com.google.gson.annotations.SerializedName


data class Plan (

  @SerializedName("_id"      ) var Id       : String? = null,
  @SerializedName("name"     ) var name     : String? = null,
  @SerializedName("price"    ) var price    : Int?    = null,
  @SerializedName("image"    ) var image    : String? = null,
  @SerializedName("duration" ) var duration : String? = null,
  @SerializedName("googleSubscriptionID" ) var googleSubscriptionID : String? = null,
  @SerializedName("appleSubscriptionID" ) var appleSubscriptionID : String? = null

)