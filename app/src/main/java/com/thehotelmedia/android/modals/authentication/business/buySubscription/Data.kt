package com.thehotelmedia.android.modals.authentication.business.buySubscription

import com.google.gson.annotations.SerializedName


data class Data (

  @SerializedName("businessProfileID" ) var businessProfileID : String? = null,
  @SerializedName("createdAt"         ) var createdAt         : String? = null,
  @SerializedName("updatedAt"         ) var updatedAt         : String? = null,
  @SerializedName("__v"               ) var _v                : Int?    = null,
  @SerializedName("id"                ) var id                : String? = null

)