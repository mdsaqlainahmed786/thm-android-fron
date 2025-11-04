package com.thehotelmedia.android.modals.authentication.verifyMobileNumber

import com.google.gson.annotations.SerializedName


data class Data (

  @SerializedName("errors"  ) var errors  : String? = null,
  @SerializedName("name"    ) var name    : String? = null,
  @SerializedName("message" ) var message : String? = null

)