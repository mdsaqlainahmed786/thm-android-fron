package com.thehotelmedia.android.modals.accountReach

import com.google.gson.annotations.SerializedName


data class Data (

  @SerializedName("_id"               ) var id                : String? = null,
  @SerializedName("businessProfileID" ) var businessProfileID : String? = null,
  @SerializedName("reachedBy"         ) var reachedBy         : String? = null,
  @SerializedName("createdAt"         ) var createdAt         : String? = null,
  @SerializedName("updatedAt"         ) var updatedAt         : String? = null,
  @SerializedName("__v"               ) var _v                : Int?    = null

)