package com.thehotelmedia.android.modals.feeds.savePost

import com.google.gson.annotations.SerializedName
import com.thehotelmedia.android.modals.feeds.savePost.Data


data class SavePostModal (

  @SerializedName("status"     ) var status     : Boolean? = null,
  @SerializedName("statusCode" ) var statusCode : Int?     = null,
  @SerializedName("message"    ) var message    : String?  = null,
  @SerializedName("data"       ) var data       : Data?    = Data()

)