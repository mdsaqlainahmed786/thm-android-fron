package com.thehotelmedia.android.modals.authentication.forgetPassword.forgetPassword

import com.google.gson.annotations.SerializedName


data class Data (

  @SerializedName("email"      ) var email : String? = null,
  @SerializedName("resetToken" ) var resetToken : String? = null

)