package com.thehotelmedia.android.modals.authentication.individual.signUp

import com.google.gson.annotations.SerializedName
import com.thehotelmedia.android.modals.authentication.individual.signUp.Data


data class IndividualSignUpModal (

  @SerializedName("status"     ) var status     : Boolean? = null,
  @SerializedName("statusCode" ) var statusCode : Int?     = null,
  @SerializedName("message"    ) var message    : String?  = null,
  @SerializedName("data"       ) var data       : Data?    = Data()

)