package com.thehotelmedia.android.modals.visitWebSite

import com.google.gson.annotations.SerializedName
import com.thehotelmedia.android.modals.visitWebSite.Data


data class WebsiteRedirectModal (

  @SerializedName("status"     ) var status     : Boolean? = null,
  @SerializedName("statusCode" ) var statusCode : Int?     = null,
  @SerializedName("message"    ) var message    : String?  = null,
  @SerializedName("data"       ) var data       : Data?    = Data()

)