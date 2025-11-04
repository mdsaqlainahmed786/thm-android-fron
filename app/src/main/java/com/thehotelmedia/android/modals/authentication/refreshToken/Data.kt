package com.thehotelmedia.android.modals.authentication.refreshToken

import com.google.gson.annotations.SerializedName


data class Data (

  @SerializedName("accessToken"  ) var accessToken  : String? = null,
  @SerializedName("refreshToken" ) var refreshToken : String? = null

)