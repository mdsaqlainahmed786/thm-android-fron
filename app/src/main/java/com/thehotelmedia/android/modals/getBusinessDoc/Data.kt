package com.thehotelmedia.android.modals.getBusinessDoc

import com.google.gson.annotations.SerializedName


data class Data (

  @SerializedName("_id"                  ) var Id                   : String? = null,
  @SerializedName("businessProfileID"    ) var businessProfileID    : String? = null,
  @SerializedName("businessRegistration" ) var businessRegistration : String? = null,
  @SerializedName("addressProof"         ) var addressProof         : String? = null,
  @SerializedName("createdAt"            ) var createdAt            : String? = null,
  @SerializedName("updatedAt"            ) var updatedAt            : String? = null,
  @SerializedName("__v"                  ) var _v                   : Int?    = null

)