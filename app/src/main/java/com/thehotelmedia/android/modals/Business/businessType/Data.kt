package com.thehotelmedia.android.modals.Business.businessType

import com.google.gson.annotations.SerializedName


data class Data (

  @SerializedName("icon"      ) var icon      : String? = null,
  @SerializedName("name"      ) var name      : String? = null,
  @SerializedName("createdAt" ) var createdAt : String? = null,
  @SerializedName("updatedAt" ) var updatedAt : String? = null,
  @SerializedName("__v"       ) var _v        : Int?    = null,
  @SerializedName("id"        ) var id        : String? = null

)