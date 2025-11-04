package com.thehotelmedia.android.modals.authentication.business.verifyEmail

import com.google.gson.annotations.SerializedName


data class BusinessVerifyEmailModal (

  @SerializedName("status"     ) var status     : Boolean? = null,
  @SerializedName("statusCode" ) var statusCode : Int?     = null,
  @SerializedName("message"    ) var message    : String?  = null,
  @SerializedName("data"       ) var data       : Data?    = Data()

)