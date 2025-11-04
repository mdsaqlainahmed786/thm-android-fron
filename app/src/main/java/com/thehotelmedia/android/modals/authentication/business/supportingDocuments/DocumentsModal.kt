package com.thehotelmedia.android.modals.authentication.business.supportingDocuments

import com.google.gson.annotations.SerializedName
import com.thehotelmedia.android.modals.authentication.business.supportingDocuments.Data


data class DocumentsModal (

  @SerializedName("status"     ) var status     : Boolean? = null,
  @SerializedName("statusCode" ) var statusCode : Int?     = null,
  @SerializedName("message"    ) var message    : String?  = null,
  @SerializedName("data"       ) var data       : Data?    = Data()

)