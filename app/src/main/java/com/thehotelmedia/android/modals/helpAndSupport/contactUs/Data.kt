package com.thehotelmedia.android.modals.helpAndSupport.contactUs

import com.google.gson.annotations.SerializedName


data class Data (

  @SerializedName("_id"       ) var Id        : String? = null,
  @SerializedName("name"      ) var name      : String? = null,
  @SerializedName("email"     ) var email     : String? = null,
  @SerializedName("message"   ) var message   : String? = null,
  @SerializedName("createdAt" ) var createdAt : String? = null,
  @SerializedName("updatedAt" ) var updatedAt : String? = null,
  @SerializedName("__v"       ) var _v        : Int?    = null

)