package com.thehotelmedia.android.modals.report

import com.google.gson.annotations.SerializedName


data class Data (

  @SerializedName("contentType" ) var contentType : String? = null,
  @SerializedName("_id"         ) var Id          : String? = null,
  @SerializedName("reason"      ) var reason      : String? = null,
  @SerializedName("reportedBy"  ) var reportedBy  : String? = null,
  @SerializedName("contentID"   ) var contentID   : String? = null,
  @SerializedName("createdAt"   ) var createdAt   : String? = null,
  @SerializedName("updatedAt"   ) var updatedAt   : String? = null,
  @SerializedName("__v"         ) var _v          : Int?    = null

)