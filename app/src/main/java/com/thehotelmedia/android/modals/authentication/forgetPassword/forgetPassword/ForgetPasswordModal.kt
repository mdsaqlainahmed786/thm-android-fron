package com.thehotelmedia.android.modals.authentication.forgetPassword.forgetPassword

import com.google.gson.annotations.SerializedName


data class ForgetPasswordModal (

  @SerializedName("status"     ) var status     : Boolean? = null,
  @SerializedName("statusCode" ) var statusCode : Int?     = null,
  @SerializedName("message"    ) var message    : String?  = null,
  @SerializedName("data"       ) var data       : Data?    = Data()

)