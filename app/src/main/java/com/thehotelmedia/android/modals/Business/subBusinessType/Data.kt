package com.thehotelmedia.android.modals.Business.subBusinessType

import com.google.gson.annotations.SerializedName


data class Data (

  @SerializedName("name"           ) var name           : String? = null,
  @SerializedName("businessTypeID" ) var businessTypeID : String? = null,
  @SerializedName("createdAt"      ) var createdAt      : String? = null,
  @SerializedName("updatedAt"      ) var updatedAt      : String? = null,
  @SerializedName("__v"            ) var _v             : Int?    = null,
  @SerializedName("id"             ) var id             : String? = null

)